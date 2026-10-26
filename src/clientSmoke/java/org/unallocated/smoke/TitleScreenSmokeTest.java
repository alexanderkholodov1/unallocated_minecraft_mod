package org.unallocated.smoke;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.TitleScreen;
import com.mojang.blaze3d.platform.InputConstants;
import org.slf4j.LoggerFactory;
import org.unallocated.client.contract.ContractClient;
import org.unallocated.client.contract.ContractScreen;
import org.unallocated.contract.ContractStore;
import org.unallocated.contract.ContractSubmission;

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
            context.waitTicks(2);
            context.takeScreenshot("unallocated-contract");
            context.getInput().pressKey(InputConstants.KEY_ESCAPE);
            context.waitForScreen(ContractScreen.class);
            context.clickScreenButton("contract.unallocated.read");
            context.waitTicks(2);
            context.takeScreenshot("unallocated-contract-uses");
            context.clickScreenButton("contract.unallocated.next");
            context.clickScreenButton("contract.unallocated.previous");
            if (Files.exists(receipt)) throw new AssertionError("Reading uses must not accept");
            context.clickScreenButton("contract.unallocated.back");
            context.getInput().typeChars("no");
            context.getInput().pressKey(InputConstants.KEY_RETURN);
            if (Files.exists(receipt) || ContractSmokeObserver.titleInitializations != 0) {
                throw new AssertionError("Invalid text must not fabricate an agreement");
            }
            context.getInput().holdControl();
            context.getInput().pressKey(InputConstants.KEY_A);
            context.getInput().releaseControl();
            String typed = " dE AcUeRdO ";
            context.getInput().typeChars(typed);
            context.getInput().pressKey(InputConstants.KEY_RETURN);
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
                context.clickScreenButton("contract.unallocated.accept");
                context.waitForScreen(TitleScreen.class);
                if (!java.util.Arrays.equals(original, Files.readAllBytes(receipt))) {
                    throw new AssertionError("Repeated agreement must preserve the original receipt");
                }
            } catch (IOException exception) {
                throw new AssertionError("Acceptance must write a readable local receipt", exception);
            }
            LoggerFactory.getLogger("unallocated-smoke").info(
                    "UNALLOCATED smoke: contract first start, data list, typed and clicked acceptance verified.");
        } else if (phase.equals("restart")) {
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
        context.takeScreenshot("unallocated-title-" + phase);
        LoggerFactory.getLogger("unallocated-smoke").info(
                "UNALLOCATED smoke: title screen reached from player JAR.");
    }
}
