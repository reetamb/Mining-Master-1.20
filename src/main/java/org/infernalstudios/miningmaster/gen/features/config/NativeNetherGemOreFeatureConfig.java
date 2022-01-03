/*
 * Copyright 2022 Infernal Studios
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.infernalstudios.miningmaster.gen.features.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.world.gen.feature.IFeatureConfig;
import net.minecraft.world.gen.feature.template.BlockMatchRuleTest;
import net.minecraft.world.gen.feature.template.RuleTest;

public class NativeNetherGemOreFeatureConfig implements IFeatureConfig {
    public static final Codec<NativeNetherGemOreFeatureConfig> CODEC = RecordCodecBuilder.create((builder) -> {
        return builder.group(
                RuleTest.CODEC.fieldOf("target").forGetter((config) -> {return config.target;}),
                BlockState.CODEC.fieldOf("state").forGetter((config) -> {return config.state;}))
                .apply(builder, NativeNetherGemOreFeatureConfig::new);
    });

    public final RuleTest target;
    public final BlockState state;

    public NativeNetherGemOreFeatureConfig(RuleTest target, BlockState state) {
        this.target = target;
        this.state = state;
    }

    public static final RuleTest NETHERRACK = new BlockMatchRuleTest(Blocks.NETHERRACK);
}
