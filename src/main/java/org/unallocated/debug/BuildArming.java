package org.unallocated.debug;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import java.util.List;
import java.util.function.Consumer;

/** Decorates the map package's completed command tree, without taking over its build command.
 * Installed after all registration callbacks, before joins. Only successful exact build-all
 * execution arms; individual sections, failed builds, and unrelated commands never arm a world.
 * Direct Java template builders call CoreRuntime.arm(server) after successful verification.
 */
public final class BuildArming {
    private BuildArming() {}
    public static <S> void install(CommandDispatcher<S> dispatcher, Consumer<S> arm) {
        CommandNode<S> root = dispatcher.getRoot().getChild("unallocated");
        if (root == null) return;
        CommandNode<S> build = root.getChild("build");
        if (build != null) wrap(build, arm);
    }
    private static <S> void wrap(CommandNode<S> parent, Consumer<S> arm) {
        for (CommandNode<S> child : List.copyOf(parent.getChildren())) {
            wrap(child, arm);
            Command<S> original = child.getCommand();
            if (original == null || original instanceof ArmingCommand<?>) continue;
            var builder = child.createBuilder();
            builder.executes(new ArmingCommand<>(original, arm));
            // Brigadier merges the command into the existing node, preserving children,
            // argument type, permissions and suggestions from the content package.
            parent.addChild(builder.build());
        }
    }
    private record ArmingCommand<S>(Command<S> original, Consumer<S> arm) implements Command<S> {
        @Override public int run(com.mojang.brigadier.context.CommandContext<S> context)
                throws com.mojang.brigadier.exceptions.CommandSyntaxException {
            int result = original.run(context);
            String input = context.getInput().trim();
            if (input.startsWith("/")) input = input.substring(1);
            if (result > 0 && input.matches("unallocated\\s+build\\s+all")) arm.accept(context.getSource());
            return result;
        }
    }
}
