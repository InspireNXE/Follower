/**
 * This file is part of Enquiry, licensed under the MIT License (MIT).
 *
 * Copyright (c) InspireNXE <http://github.com/InspireNXE/>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.inspirenxe.follower;

import static org.spongepowered.api.util.command.args.GenericArguments.bool;
import static org.spongepowered.api.util.command.args.GenericArguments.optional;
import static org.spongepowered.api.util.command.args.GenericArguments.playerOrSource;
import static org.spongepowered.api.util.command.args.GenericArguments.seq;
import static org.spongepowered.api.util.command.args.GenericArguments.string;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.data.manipulator.DisplayNameData;
import org.spongepowered.api.data.manipulator.entity.SkinData;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.player.Player;
import org.spongepowered.api.event.Subscribe;
import org.spongepowered.api.event.state.ServerAboutToStartEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.util.command.CommandException;
import org.spongepowered.api.util.command.CommandResult;
import org.spongepowered.api.util.command.CommandSource;
import org.spongepowered.api.util.command.args.CommandContext;
import org.spongepowered.api.util.command.args.GenericArguments;
import org.spongepowered.api.util.command.source.CommandBlockSource;
import org.spongepowered.api.util.command.spec.CommandExecutor;
import org.spongepowered.api.util.command.spec.CommandSpec;
import org.spongepowered.api.world.Location;
import org.spongepowered.common.entity.living.human.EntityHuman;

import java.io.IOException;
import java.util.UUID;

@Plugin(id = "follower", name = "Follower", version = "0.1")
@NonnullByDefault
public class Follower {

    @Inject public Game game;
    @Inject public Logger logger;

    @Subscribe
    public void onServerAboutToStart(ServerAboutToStartEvent event) throws IOException {
        event.getGame().getCommandDispatcher().register(this, CommandSpec.builder()
                .description(Texts.of("Spawns a Human!"))
                .arguments(seq(string(Texts.of("name")), optional(playerOrSource(Texts.of("player"), event.getGame())), optional(string
                        (Texts.of("skin"))), optional(GenericArguments.location(Texts.of("loc"), event.getGame())), optional(bool(Texts.of("obedient")
                ))))
                .permission("humanity.command.spawn")
                .executor(new CommandExecutor() {
                    @Override
                    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
                        final Player player = args.<Player>getOne("player").orNull();
                        Optional<Location> optLoc = args.getOne("loc");
                        Location loc = null;
                        final boolean obedient = args.<Boolean>getOne("obedient").or(true);
                        if (!optLoc.isPresent() && player != null) {
                            loc = player.getLocation();
                        }
                        if (loc == null) {
                            if (!(src instanceof CommandBlockSource)) {
                                throw new CommandException(Texts.of("To spawn a human you must provide the world and coordinates (x, y, z) or be "
                                        + "in-game to use your current location"));
                            }
                            loc = ((CommandBlockSource) src).getLocation().add(1, 0, 0);
                        }
                        final Optional<Entity> optEntity = loc.getExtent().createEntity(EntityTypes.HUMAN, loc.getPosition());
                        if (!optEntity.isPresent()) {
                            throw new CommandException(Texts.of("Failed to spawn human!"));
                        }
                        optEntity.get().offer(optEntity.get().getOrCreate(DisplayNameData.class).get().setDisplayName(Texts.of(args.<String>getOne
                                ("name").get())));
                        final Optional<String> optSkinUuid = args.getOne("skin");
                        if (optSkinUuid.isPresent()) {
                            try {
                                final UUID skinUuid = UUID.fromString(optSkinUuid.get());
                                optEntity.get().offer(optEntity.get().getOrCreate(SkinData.class).get().setValue(skinUuid));
                            } catch (NumberFormatException nfe) {
                                throw new CommandException(Texts.of("UUID provided was not in a valid format. Ex. "
                                        + "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"));
                            }
                        }
                        final EntityHuman human = (EntityHuman) optEntity.get();
                        human.setOwner(obedient ? (EntityLivingBase) player : null);
                        final int random = (int) (Math.random() * 100) % 3;
                        switch (random) {
                            case 0:
                                human.setCurrentItemOrArmor(0, new ItemStack(Items.diamond_sword));
                                break;
                            case 1:
                                human.setCurrentItemOrArmor(0, new ItemStack(Items.bow));
                                break;
                        }
                        loc.getExtent().spawnEntity(optEntity.get());
                        return CommandResult.success();
                    }
                })
                .build(), "spawn");
    }
}
