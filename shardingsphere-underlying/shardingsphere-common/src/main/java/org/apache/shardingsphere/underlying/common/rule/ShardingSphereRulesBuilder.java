/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.underlying.common.rule;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.shardingsphere.spi.ShardingSphereServiceLoader;
import org.apache.shardingsphere.spi.order.OrderedSPIRegistry;
import org.apache.shardingsphere.underlying.common.config.RuleConfiguration;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * ShardingSphere rule builder.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ShardingSphereRulesBuilder {
    
    static {
        ShardingSphereServiceLoader.register(ShardingSphereRuleBuilder.class);
    }
    
    /**
     * Build rules.
     *
     * @param ruleConfigurations rule configurations
     * @param dataSourceNames data source names
     * @return rules
     */
    @SuppressWarnings("unchecked")
    public static Collection<ShardingSphereRule> build(final Collection<RuleConfiguration> ruleConfigurations, final Collection<String> dataSourceNames) {
        return OrderedSPIRegistry.getRegisteredServices(
                ruleConfigurations, ShardingSphereRuleBuilder.class).entrySet().stream().map(entry -> entry.getValue().build(entry.getKey(), dataSourceNames)).collect(Collectors.toList());
    }
}
