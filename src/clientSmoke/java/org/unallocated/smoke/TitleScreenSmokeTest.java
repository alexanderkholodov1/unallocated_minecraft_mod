package org.unallocated.smoke;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModOrigin;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import com.mojang.blaze3d.platform.InputConstants;
import org.slf4j.LoggerFactory;
import org.unallocated.client.contract.ContractClient;
import org.unallocated.client.contract.ContractScreen;
import org.unallocated.contract.ContractStore;
import org.unallocated.contract.ContractSubmission;

/** The only gametest entrypoint: the contract runs first, then each package's checks from the title. */
@SuppressWarnings("UnstableApiUsage")
public final class TitleScreenSmokeTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        if (FabricLoader.getInstance().getEnvironmentType() != EnvType.CLIENT) {
            throw new AssertionError("Expected a real client environment");
        }
        var mod = FabricLoader.getInstance().getModContainer("unallocated").orElseThrow();
        List<Path> origins = mod.getOrigin().getPaths();
        Path expected = Path.of(System.getProperty("unallocated.smoke.jar"));
        try {
            if (origins.size() != 1 || !Files.isRegularFile(origins.getFirst())
                    || Files.mismatch(expected, origins.getFirst()) != -1) {
                throw new AssertionError("Client must load the exact player JAR, got " + origins);
            }
        } catch (IOException exception) {
            throw new AssertionError("Could not verify the loaded player JAR", exception);
        }
        Path gameDirectory = FabricLoader.getInstance().getGameDir();
        Path receipt = gameDirectory.resolve(ContractStore.RELATIVE_PATH);
        String phase = System.getProperty("unallocated.smoke.contractPhase", "first");
        if (phase.equals("first")) {
            context.waitForScreen(ContractScreen.class);
            if (ContractSmokeObserver.titleInitializations != 0 || Files.exists(receipt)) {
                throw new AssertionError("First start must show the contract before any title initialization or receipt");
            }
            assertAgreementInput(context, "", "first-start");
            context.waitTicks(2);
            context.takeScreenshot("unallocated-contract");
            context.getInput().pressKey(InputConstants.KEY_ESCAPE);
            context.waitForScreen(ContractScreen.class);
            assertAgreementInput(context, "", "after-escape-on-agreement");
            // Use real mouse dispatch as well as Fabric's onPress helper: a callback
            // rebuilding this screen must not leave focus on the removed Back button.
            clickWithMouse(context, "contract.unallocated.read");
            clickWithMouse(context, "contract.unallocated.back");
            assertAgreementInput(context, "", "after-mouse-back");
            context.clickScreenButton("contract.unallocated.read");
            context.getInput().pressKey(InputConstants.KEY_ESCAPE);
            assertAgreementInput(context, "", "after-escape-from-uses");
            // A rejected response must be replaceable by typing immediately, without a shortcut.
            context.clickScreenButton("contract.unallocated.read");
            context.waitTicks(2);
            context.takeScreenshot("unallocated-contract-uses");
            context.clickScreenButton("contract.unallocated.next");
            context.clickScreenButton("contract.unallocated.previous");
            if (Files.exists(receipt)) throw new AssertionError("Reading uses must not accept");
            context.clickScreenButton("contract.unallocated.back");
            assertAgreementInput(context, "", "after-helper-back");
            context.getInput().typeChars("no");
            assertAgreementInput(context, "no", "after-invalid-typing");
            context.getInput().pressKey(InputConstants.KEY_RETURN);
            if (Files.exists(receipt) || ContractSmokeObserver.titleInitializations != 0) {
                throw new AssertionError("Invalid text must not fabricate an agreement");
            }
            assertAgreementInput(context, "no", "after-invalid-enter");
            context.runOnClient(client -> {
                if (!((EditBox) client.gui.screen().getFocused()).getHighlighted().equals("no")) {
                    throw new AssertionError("Rejected text must be selected for immediate replacement");
                }
            });
            String typed = " dE AcUeRdO ";
            context.getInput().typeChars(typed);
            assertAgreementInput(context, typed, "after-rejected-text-replacement");
            context.getInput().pressKey(InputConstants.KEY_NUMPADENTER);
            context.waitForScreen(TitleScreen.class);
            try {
                var saved = new ContractStore(gameDirectory).read().orElseThrow();
                if (saved.submission().method() != ContractSubmission.Method.TYPED
                        || !saved.submission().typedText().equals(typed)) {
                    throw new AssertionError("Typed agreement must preserve method and exact original text");
                }
                byte[] original = Files.readAllBytes(receipt);
                // Exercise the other input adapter through the actual button, preserving the first receipt.
                context.setScreen(() -> new ContractScreen(new TitleScreen()));
                assertAgreementInput(context, "", "reopened-after-keyboard-input");
                clickWithMouse(context, "contract.unallocated.accept");
                context.waitForScreen(TitleScreen.class);
                if (!java.util.Arrays.equals(original, Files.readAllBytes(receipt))) {
                    throw new AssertionError("Repeated agreement must preserve the original receipt");
                }
            } catch (IOException exception) {
                throw new AssertionError("Acceptance must write a readable local receipt", exception);
            }
            LoggerFactory.getLogger("unallocated-smoke").info(
                    "UNALLOCATED smoke: contract first start, data list, typed and clicked acceptance verified.");
        } else if ((phase.equals("restart") || phase.equals("story") || phase.equals("play"))) {
            context.waitForScreen(TitleScreen.class);
            if (ContractSmokeObserver.contractInitializations != 0 || !Files.isRegularFile(receipt)
                    || context.computeOnClient(client -> ContractClient.receipt().isEmpty())) {
                throw new AssertionError("Second process must load the receipt and skip the contract entirely");
            }
            LoggerFactory.getLogger("unallocated-smoke").info(
                    "UNALLOCATED smoke: contract skipped on second process with saved acceptance.");
        } else {
            throw new AssertionError("Unknown contract smoke phase: " + phase);
        }
        context.waitTicks(2);
        verifyBundledModules();
        context.takeScreenshot("unallocated-title-" + phase);
        LoggerFactory.getLogger("unallocated-smoke").info(
                "UNALLOCATED smoke: title screen reached from player JAR.");
        if (phase.equals("story")) StorySmokeTest.run(context);
        if (phase.equals("play")) PlaySmokeTest.run(context);
        if (phase.equals("first")) {
            CoreSessionSmokeTest.run(context);
            InterfaceEffectsSmokeTest.run(context);
            new SpyglassBaitSmokeTest().runTest(context);
            MapTourSmokeTest.run(context);
            EndingSmokeTest.run(context);
            AudioSmokeTest.run(context);
            LauncherSmokeTest.run(context);
        }
    }

    /** Every player module must come from inside UNALLOCATED; the helper only supplies the test runner. */
    private static void verifyBundledModules() {
        if (!ContractSmokeObserver.titleHookCalled || CommandRegistrationCallback.EVENT == null
                || ServerLifecycleEvents.SERVER_STARTED == null
                || PayloadTypeRegistry.clientboundPlay() == null
                || ResourceLoader.get(PackType.CLIENT_RESOURCES) == null) {
            throw new AssertionError("Bundled API interfaces or screen hook unavailable");
        }
        var loader = FabricLoader.getInstance();
        String externalApi = System.getProperty("unallocated.smoke.api", "none");
        boolean cleanInstall = externalApi.equals("none");
        if (cleanInstall && loader.isModLoaded("fabric-api")) {
            throw new AssertionError("Full Fabric API masks clean player packaging");
        }
        if (!cleanInstall) {
            var api = loader.getModContainer("fabric-api").orElseThrow();
            if (!api.getMetadata().getVersion().getFriendlyString().equals(externalApi)) {
                throw new AssertionError("Unexpected external Fabric API version");
            }
            LoggerFactory.getLogger("unallocated-smoke").info(
                    "UNALLOCATED smoke: external Fabric API {} loaded.", externalApi);
        }
        String expectedModules = System.getProperty("unallocated.smoke.modules", "");
        if (expectedModules.isBlank()) {
            throw new AssertionError("Missing bundled module expectations");
        }
        int expectedCount = 0;
        for (String moduleSpec : expectedModules.split(",")) {
            String[] spec = moduleSpec.split("=", 2);
            var loaded = loader.getModContainer(spec[0]).orElseThrow();
            if (!loaded.getMetadata().getVersion().getFriendlyString().equals(spec[1])) {
                throw new AssertionError("Wrong version selected for " + spec[0]);
            }
            if (cleanInstall && (loaded.getOrigin().getKind() != ModOrigin.Kind.NESTED
                    || !loaded.getOrigin().getParentModId().equals("unallocated"))) {
                throw new AssertionError("Player module supplied outside player JAR: " + spec[0]);
            }
            expectedCount++;
        }
        if (cleanInstall) {
            long loadedCount = loader.getAllMods().stream().filter(loaded -> {
                String id = loaded.getMetadata().getId();
                return id.startsWith("fabric-") && !id.equals("fabric-client-gametest-api-v1");
            }).count();
            if (loadedCount != expectedCount) {
                throw new AssertionError("Unexpected Fabric modules in clean client: " + loadedCount);
            }
        }
        LoggerFactory.getLogger("unallocated-smoke").info(
                "UNALLOCATED smoke: bundled Fabric API modules verified; screen hook verified.");
    }

    private static void assertAgreementInput(ClientGameTestContext context, String expected, String stage) {
        String failure = context.computeOnClient(client -> {
            var screen = client.gui.screen();
            if (!(screen instanceof ContractScreen) || !(screen.getFocused() instanceof EditBox field)
                    || !field.isFocused() || !screen.children().contains(field)) {
                return "Agreement field must have focus without clicking it at " + stage;
            }
            if (!field.getValue().equals(expected)) {
                return "Agreement text was lost or replacement failed at " + stage + ": expected [" + expected
                        + "], got [" + field.getValue() + "]";
            }
            return null;
        });
        if (failure != null) {
            context.takeScreenshot("unallocated-contract-input-failure-" + stage);
            throw new AssertionError(failure);
        }
    }

    private static void clickWithMouse(ClientGameTestContext context, String translationKey) {
        double[] position = context.computeOnClient(client -> {
            var screen = client.gui.screen();
            String label = Component.translatable(translationKey).getString();
            Button button = screen.children().stream()
                    .filter(child -> child instanceof Button candidate && candidate.getMessage().getString().equals(label))
                    .map(child -> (Button) child).findFirst().orElseThrow();
            return new double[] {
                (button.getX() + button.getWidth() / 2.0) * client.getWindow().getScreenWidth() / screen.width,
                (button.getY() + button.getHeight() / 2.0) * client.getWindow().getScreenHeight() / screen.height
            };
        });
        context.getInput().setCursorPos(position[0], position[1]);
        context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
    }
}
