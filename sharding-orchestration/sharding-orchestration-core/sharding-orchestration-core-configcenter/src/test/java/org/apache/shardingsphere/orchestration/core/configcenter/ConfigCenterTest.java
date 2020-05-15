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

package org.apache.shardingsphere.orchestration.core.configcenter;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.shardingsphere.encrypt.api.config.EncryptRuleConfiguration;
import org.apache.shardingsphere.encrypt.api.config.EncryptorRuleConfiguration;
import org.apache.shardingsphere.masterslave.api.config.MasterSlaveRuleConfiguration;
import org.apache.shardingsphere.orchestration.center.ConfigCenterRepository;
import org.apache.shardingsphere.shadow.api.config.ShadowRuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.ShardingRuleConfiguration;
import org.apache.shardingsphere.underlying.common.auth.Authentication;
import org.apache.shardingsphere.underlying.common.yaml.config.YamlRootRuleConfigurations;
import org.apache.shardingsphere.underlying.common.auth.yaml.config.YamlAuthenticationConfiguration;
import org.apache.shardingsphere.sharding.yaml.constructor.YamlRootRuleConfigurationsConstructor;
import org.apache.shardingsphere.underlying.common.auth.yaml.swapper.AuthenticationYamlSwapper;
import org.apache.shardingsphere.underlying.common.yaml.swapper.RuleRootConfigurationsYamlSwapper;
import org.apache.shardingsphere.underlying.common.config.DataSourceConfiguration;
import org.apache.shardingsphere.underlying.common.config.RuleConfiguration;
import org.apache.shardingsphere.underlying.common.config.properties.ConfigurationPropertyKey;
import org.apache.shardingsphere.underlying.common.yaml.engine.YamlEngine;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.sql.DataSource;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Slf4j
@RunWith(MockitoJUnitRunner.class)
public final class ConfigCenterTest {

    private static final String DATA_SOURCE_YAML = readYamlFileIntoString("yaml/data-source-yaml.yml");

    private static final String DATA_SOURCE_PARAMETER_YAML = readYamlFileIntoString("yaml/data-source-parameter-yaml.yml");

    private static final String SHARDING_RULE_YAML = readYamlFileIntoString("yaml/sharding-rule-yaml.yml");

    private static final String MASTER_SLAVE_RULE_YAML = readYamlFileIntoString("yaml/master-slave-rule-yaml.yml");

    private static final String ENCRYPT_RULE_YAML = readYamlFileIntoString("yaml/encrypt-rule-yaml.yml");

    private static final String SHADOW_RULE_YAML = readYamlFileIntoString("yaml/shadow-rule-yaml.yml");

    private static final String AUTHENTICATION_YAML = readYamlFileIntoString("yaml/authentication-yaml.yml");

    private static final String PROPS_YAML = readYamlFileIntoString("yaml/props-yaml.yml");

    private static final String SHARDING_RULE_YAML_DEFAULT_TABLE_STRATEGY_NONE = readYamlFileIntoString("yaml/sharding-rule-yaml-default-table-strategy-none.yml");

    private static final String DATA_SOURCE_YAML_WITH_CONNECTION_INIT_SQLS = readYamlFileIntoString("yaml/data-source-yaml-with-connection-init-sqls.yml");

    @Mock
    private ConfigCenterRepository configCenterRepository;
    
    @Test
    public void assertPersistConfigurationForShardingRuleWithoutAuthenticationAndIsNotOverwriteAndConfigurationIsExisted() {
        when(configCenterRepository.get("/test/config/schema/sharding_db/datasource")).thenReturn(DATA_SOURCE_YAML);
        when(configCenterRepository.get("/test/config/schema/sharding_db/rule")).thenReturn(SHARDING_RULE_YAML);
        when(configCenterRepository.get("/test/config/props")).thenReturn(PROPS_YAML);
        ConfigCenter configurationService = new ConfigCenter("test", configCenterRepository);
        configurationService.persistConfigurations("sharding_db", createDataSourceConfigurations(), createRuleConfigurations(), null, createProperties(), false);
        verify(configCenterRepository, times(0)).persist(eq("/test/config/schema/sharding_db/datasource"), ArgumentMatchers.any());
        verify(configCenterRepository, times(0)).persist("/test/config/schema/sharding_db/rule", SHARDING_RULE_YAML);
        verify(configCenterRepository, times(0)).persist("/test/config/props", PROPS_YAML);
    }
    
    @Test
    public void assertMoreShardingSchema() {
        when(configCenterRepository.get("/test/config/schema/sharding_db/datasource")).thenReturn(DATA_SOURCE_YAML);
        when(configCenterRepository.get("/test/config/schema/sharding_db/rule")).thenReturn(SHARDING_RULE_YAML);
        when(configCenterRepository.get("/test/config/props")).thenReturn(PROPS_YAML);
        when(configCenterRepository.get("/test/config/schema")).thenReturn("myTest1,myTest2");
        ConfigCenter configurationService = new ConfigCenter("test", configCenterRepository);
        configurationService.persistConfigurations("sharding_db", createDataSourceConfigurations(), createRuleConfigurations(), null, createProperties(), false);
        verify(configCenterRepository, times(1)).persist("/test/config/schema", "myTest1,myTest2,sharding_db");
    }
    
    @Test
    public void assertMoreAndContainsShardingSchema() {
        when(configCenterRepository.get("/test/config/schema/sharding_db/datasource")).thenReturn(DATA_SOURCE_YAML);
        when(configCenterRepository.get("/test/config/schema/sharding_db/rule")).thenReturn(SHARDING_RULE_YAML);
        when(configCenterRepository.get("/test/config/props")).thenReturn(PROPS_YAML);
        when(configCenterRepository.get("/test/config/schema")).thenReturn("myTest1,sharding_db");
        ConfigCenter configurationService = new ConfigCenter("test", configCenterRepository);
        configurationService.persistConfigurations("sharding_db", createDataSourceConfigurations(), createRuleConfigurations(), null, createProperties(), false);
        verify(configCenterRepository, times(0)).persist("/test/config/schema", "myTest1,sharding_db");
    }
    
    @Test
    public void assertPersistConfigurationForShardingRuleWithoutAuthenticationAndIsNotOverwriteAndConfigurationIsNotExisted() {
        ConfigCenter configurationService = new ConfigCenter("test", configCenterRepository);
        configurationService.persistConfigurations("sharding_db", createDataSourceConfigurations(), createRuleConfigurations(), null, createProperties(), false);
        verify(configCenterRepository).persist(eq("/test/config/schema/sharding_db/datasource"), ArgumentMatchers.any());
        verify(configCenterRepository).persist("/test/config/schema/sharding_db/rule", SHARDING_RULE_YAML);
        verify(configCenterRepository).persist("/test/config/props", PROPS_YAML);
    }
    
    @Test
    public void assertPersistConfigurationForShardingRuleWithoutAuthenticationAndIsOverwrite() {
        ConfigCenter configurationService = new ConfigCenter("test", configCenterRepository);
        configurationService.persistConfigurations("sharding_db", createDataSourceConfigurations(), createRuleConfigurations(), null, createProperties(), true);
        verify(configCenterRepository).persist(eq("/test/config/schema/sharding_db/datasource"), ArgumentMatchers.any());
        verify(configCenterRepository).persist("/test/config/schema/sharding_db/rule", SHARDING_RULE_YAML);
        verify(configCenterRepository).persist("/test/config/props", PROPS_YAML);
    }
    
    @Test
    public void assertPersistConfigurationForMasterSlaveRuleWithoutAuthenticationAndIsNotOverwriteAndConfigurationIsExisted() {
        when(configCenterRepository.get("/test/config/schema/sharding_db/datasource")).thenReturn(DATA_SOURCE_YAML);
        when(configCenterRepository.get("/test/config/schema/sharding_db/rule")).thenReturn(MASTER_SLAVE_RULE_YAML);
        when(configCenterRepository.get("/test/config/props")).thenReturn(PROPS_YAML);
        ConfigCenter configurationService = new ConfigCenter("test", configCenterRepository);
        configurationService.persistConfigurations("sharding_db", createDataSourceConfigurations(), createMasterSlaveRuleConfiguration(), null, createProperties(), false);
        verify(configCenterRepository, times(0)).persist(eq("/test/config/schema/sharding_db/datasource"), ArgumentMatchers.any());
        verify(configCenterRepository, times(0)).persist("/test/config/schema/sharding_db/rule", MASTER_SLAVE_RULE_YAML);
        verify(configCenterRepository, times(0)).persist("/test/config/props", PROPS_YAML);
    }
    
    @Test
    public void assertPersistConfigurationForMasterSlaveRuleWithoutAuthenticationAndIsNotOverwriteAndConfigurationIsNotExisted() {
        ConfigCenter configurationService = new ConfigCenter("test", configCenterRepository);
        configurationService.persistConfigurations("sharding_db", createDataSourceConfigurations(), createMasterSlaveRuleConfiguration(), null, createProperties(), false);
        verify(configCenterRepository).persist(eq("/test/config/schema/sharding_db/datasource"), ArgumentMatchers.any());
        verify(configCenterRepository).persist("/test/config/schema/sharding_db/rule", MASTER_SLAVE_RULE_YAML);
        verify(configCenterRepository).persist("/test/config/props", PROPS_YAML);
    }
    
    @Test
    public void assertPersistConfigurationForMasterSlaveRuleWithoutAuthenticationAndIsOverwrite() {
        ConfigCenter configurationService = new ConfigCenter("test", configCenterRepository);
        configurationService.persistConfigurations("sharding_db", createDataSourceConfigurations(), createMasterSlaveRuleConfiguration(), null, createProperties(), true);
        verify(configCenterRepository).persist(eq("/test/config/schema/sharding_db/datasource"), ArgumentMatchers.any());
        verify(configCenterRepository).persist("/test/config/schema/sharding_db/rule", MASTER_SLAVE_RULE_YAML);
        verify(configCenterRepository).persist("/test/config/props", PROPS_YAML);
    }
    
    @Test
    public void assertPersistConfigurationForShardingRuleWithAuthenticationAndIsNotOverwriteAndConfigurationIsExisted() {
        when(configCenterRepository.get("/test/config/schema/sharding_db/datasource")).thenReturn(DATA_SOURCE_PARAMETER_YAML);
        when(configCenterRepository.get("/test/config/schema/sharding_db/rule")).thenReturn(SHARDING_RULE_YAML);
        when(configCenterRepository.get("/test/config/authentication")).thenReturn(AUTHENTICATION_YAML);
        when(configCenterRepository.get("/test/config/props")).thenReturn(PROPS_YAML);
        ConfigCenter configurationService = new ConfigCenter("test", configCenterRepository);
        configurationService.persistConfigurations("sharding_db", createDataSourceConfigurations(), createRuleConfigurations(), createAuthentication(), createProperties(), false);
        verify(configCenterRepository, times(0)).persist(eq("/test/config/schema/sharding_db/datasource"), ArgumentMatchers.any());
        verify(configCenterRepository, times(0)).persist("/test/config/schema/sharding_db/rule", SHARDING_RULE_YAML);
        verify(configCenterRepository, times(0)).persist("/test/config/authentication", AUTHENTICATION_YAML);
        verify(configCenterRepository, times(0)).persist("/test/config/props", PROPS_YAML);
    }
    
    @Test
    public void assertPersistConfigurationForShardingRuleWithAuthenticationAndIsNotOverwriteAndConfigurationIsNotExisted() {
        ConfigCenter configurationService = new ConfigCenter("test", configCenterRepository);
        configurationService.persistConfigurations("sharding_db", createDataSourceConfigurations(), createRuleConfigurations(), createAuthentication(), createProperties(), false);
        verify(configCenterRepository).persist(eq("/test/config/schema/sharding_db/datasource"), ArgumentMatchers.any());
        verify(configCenterRepository).persist("/test/config/schema/sharding_db/rule", SHARDING_RULE_YAML);
        verify(configCenterRepository).persist("/test/config/authentication", AUTHENTICATION_YAML);
        verify(configCenterRepository).persist("/test/config/props", PROPS_YAML);
    }
    
    @Test
    public void assertPersistConfigurationForShardingRuleWithAuthenticationAndIsOverwrite() {
        ConfigCenter configurationService = new ConfigCenter("test", configCenterRepository);
        configurationService.persistConfigurations("sharding_db", createDataSourceConfigurations(), createRuleConfigurations(), createAuthentication(), createProperties(), true);
        verify(configCenterRepository).persist(eq("/test/config/schema/sharding_db/datasource"), ArgumentMatchers.any());
        verify(configCenterRepository).persist("/test/config/schema/sharding_db/rule", SHARDING_RULE_YAML);
        verify(configCenterRepository).persist("/test/config/authentication", AUTHENTICATION_YAML);
        verify(configCenterRepository).persist("/test/config/props", PROPS_YAML);
    }
    
    @Test
    public void assertPersistConfigurationForMasterSlaveRuleWithAuthenticationAndIsNotOverwriteAndConfigurationIsExisted() {
        when(configCenterRepository.get("/test/config/schema/sharding_db/datasource")).thenReturn(DATA_SOURCE_PARAMETER_YAML);
        when(configCenterRepository.get("/test/config/schema/sharding_db/rule")).thenReturn(MASTER_SLAVE_RULE_YAML);
        when(configCenterRepository.get("/test/config/authentication")).thenReturn(AUTHENTICATION_YAML);
        when(configCenterRepository.get("/test/config/props")).thenReturn(PROPS_YAML);
        ConfigCenter configurationService = new ConfigCenter("test", configCenterRepository);
        configurationService.persistConfigurations("sharding_db", createDataSourceConfigurations(), createMasterSlaveRuleConfiguration(), createAuthentication(), createProperties(), false);
        verify(configCenterRepository, times(0)).persist(eq("/test/config/schema/sharding_db/datasource"), ArgumentMatchers.any());
        verify(configCenterRepository, times(0)).persist("/test/config/schema/sharding_db/rule", MASTER_SLAVE_RULE_YAML);
        verify(configCenterRepository, times(0)).persist("/test/config/authentication", AUTHENTICATION_YAML);
        verify(configCenterRepository, times(0)).persist("/test/config/props", PROPS_YAML);
    }
    
    @Test
    public void assertPersistConfigurationForMasterSlaveRuleWithAuthenticationAndIsNotOverwriteAndConfigurationIsNotExisted() {
        ConfigCenter configurationService = new ConfigCenter("test", configCenterRepository);
        configurationService.persistConfigurations("sharding_db",
                createDataSourceConfigurations(), createMasterSlaveRuleConfiguration(), createAuthentication(), createProperties(), false);
        verify(configCenterRepository).persist(eq("/test/config/schema/sharding_db/datasource"), ArgumentMatchers.any());
        verify(configCenterRepository).persist("/test/config/schema/sharding_db/rule", MASTER_SLAVE_RULE_YAML);
        verify(configCenterRepository).persist("/test/config/authentication", AUTHENTICATION_YAML);
        verify(configCenterRepository).persist("/test/config/props", PROPS_YAML);
    }
    
    @Test
    public void assertPersistConfigurationForMasterSlaveRuleWithAuthenticationAndIsOverwrite() {
        ConfigCenter configurationService = new ConfigCenter("test", configCenterRepository);
        configurationService.persistConfigurations("sharding_db", createDataSourceConfigurations(), createMasterSlaveRuleConfiguration(), createAuthentication(), createProperties(), true);
        verify(configCenterRepository).persist(eq("/test/config/schema/sharding_db/datasource"), ArgumentMatchers.any());
        verify(configCenterRepository).persist("/test/config/schema/sharding_db/rule", MASTER_SLAVE_RULE_YAML);
        verify(configCenterRepository).persist("/test/config/authentication", AUTHENTICATION_YAML);
        verify(configCenterRepository).persist("/test/config/props", PROPS_YAML);
    }
    
    @Test
    public void assertPersistConfigurationForEncrypt() {
        ConfigCenter configurationService = new ConfigCenter("test", configCenterRepository);
        configurationService.persistConfigurations("sharding_db", createDataSourceConfigurations(), createEncryptRuleConfiguration(), null, createProperties(), true);
        verify(configCenterRepository).persist(eq("/test/config/schema/sharding_db/datasource"), ArgumentMatchers.any());
        verify(configCenterRepository).persist("/test/config/schema/sharding_db/rule", ENCRYPT_RULE_YAML);
    }
    
    @Test
    public void assertNullRuleConfiguration() {
        ConfigCenter configurationService = new ConfigCenter("test", configCenterRepository);
        configurationService.persistConfigurations("sharding_db", createDataSourceConfigurations(), Collections.emptyList(), null, createProperties(), true);
    }
    
    @Test
    @Ignore
    public void assertPersistConfigurationForShadow() {
        ConfigCenter configurationService = new ConfigCenter("test", configCenterRepository);
        configurationService.persistConfigurations("sharding_db", createDataSourceConfigurations(), createShadowRuleConfiguration(), null, createProperties(), true);
        verify(configCenterRepository).persist(eq("/test/config/schema/sharding_db/datasource"), ArgumentMatchers.any());
        verify(configCenterRepository).persist("/test/config/schema/sharding_db/rule", SHADOW_RULE_YAML);
    }
    
    private Map<String, DataSourceConfiguration> createDataSourceConfigurations() {
        return createDataSourceMap().entrySet().stream().collect(Collectors.toMap(Entry::getKey, entry -> DataSourceConfiguration.getDataSourceConfiguration(entry.getValue())));
    }
    
    private DataSourceConfiguration createDataSourceConfiguration(final DataSource dataSource) {
        return DataSourceConfiguration.getDataSourceConfiguration(dataSource);
    }
    
    private Map<String, DataSource> createDataSourceMap() {
        Map<String, DataSource> result = new LinkedHashMap<>(2, 1);
        result.put("ds_0", createDataSource("ds_0"));
        result.put("ds_1", createDataSource("ds_1"));
        return result;
    }
    
    private DataSource createDataSource(final String name) {
        BasicDataSource result = new BasicDataSource();
        result.setDriverClassName("com.mysql.jdbc.Driver");
        result.setUrl("jdbc:mysql://localhost:3306/" + name);
        result.setUsername("root");
        result.setPassword("root");
        return result;
    }
    
    private Collection<RuleConfiguration> createRuleConfigurations() {
        return new RuleRootConfigurationsYamlSwapper().swap(YamlEngine.unmarshal(SHARDING_RULE_YAML, YamlRootRuleConfigurations.class, new YamlRootRuleConfigurationsConstructor()));
    }
    
    private Collection<RuleConfiguration> createMasterSlaveRuleConfiguration() {
        return new RuleRootConfigurationsYamlSwapper().swap(YamlEngine.unmarshal(MASTER_SLAVE_RULE_YAML, YamlRootRuleConfigurations.class));
    }
    
    private Collection<RuleConfiguration> createEncryptRuleConfiguration() {
        return new RuleRootConfigurationsYamlSwapper().swap(YamlEngine.unmarshal(ENCRYPT_RULE_YAML, YamlRootRuleConfigurations.class));
    }
    
    private Collection<RuleConfiguration> createShadowRuleConfiguration() {
        return new RuleRootConfigurationsYamlSwapper().swap(YamlEngine.unmarshal(SHADOW_RULE_YAML, YamlRootRuleConfigurations.class));
    }
    
    private Authentication createAuthentication() {
        return new AuthenticationYamlSwapper().swap(YamlEngine.unmarshal(AUTHENTICATION_YAML, YamlAuthenticationConfiguration.class));
    }
    
    private Properties createProperties() {
        Properties result = new Properties();
        result.put(ConfigurationPropertyKey.SQL_SHOW.getKey(), Boolean.FALSE);
        return result;
    }
    
    @Test
    public void assertLoadDataSourceConfigurations() {
        when(configCenterRepository.get("/test/config/schema/sharding_db/datasource")).thenReturn(DATA_SOURCE_YAML);
        ConfigCenter configurationService = new ConfigCenter("test", configCenterRepository);
        Map<String, DataSourceConfiguration> actual = configurationService.loadDataSourceConfigurations("sharding_db");
        assertThat(actual.size(), is(2));
        assertDataSourceConfiguration(actual.get("ds_0"), createDataSourceConfiguration(createDataSource("ds_0")));
        assertDataSourceConfiguration(actual.get("ds_1"), createDataSourceConfiguration(createDataSource("ds_1")));
    }
    
    private void assertDataSourceConfiguration(final DataSourceConfiguration actual, final DataSourceConfiguration expected) {
        assertThat(actual.getDataSourceClassName(), is(expected.getDataSourceClassName()));
        assertThat(actual.getProperties().get("url"), is(expected.getProperties().get("url")));
        assertThat(actual.getProperties().get("username"), is(expected.getProperties().get("username")));
        assertThat(actual.getProperties().get("password"), is(expected.getProperties().get("password")));
    }
    
    @Test
    public void assertIsShardingRule() {
        when(configCenterRepository.get("/test/config/schema/sharding_db/rule")).thenReturn(SHARDING_RULE_YAML);
        ConfigCenter configurationService = new ConfigCenter("test", configCenterRepository);
        assertTrue(configurationService.isShardingRule("sharding_db"));
    }

    @Test
    public void assertIsShardingRuleWithDefaultTableStrategyNone() {
        when(configCenterRepository.get("/test/config/schema/sharding_db/rule")).thenReturn(SHARDING_RULE_YAML_DEFAULT_TABLE_STRATEGY_NONE);
        ConfigCenter configurationService = new ConfigCenter("test", configCenterRepository);
        assertTrue(configurationService.isShardingRule("sharding_db"));
    }
    
    @Test
    public void assertIsEncryptRule() {
        when(configCenterRepository.get("/test/config/schema/sharding_db/rule")).thenReturn(ENCRYPT_RULE_YAML);
        ConfigCenter configurationService = new ConfigCenter("test", configCenterRepository);
        assertTrue(configurationService.isEncryptRule("sharding_db"));
    }
    
    @Test
    public void assertIsShadowRule() {
        when(configCenterRepository.get("/test/config/schema/sharding_db/rule")).thenReturn(SHADOW_RULE_YAML);
        ConfigCenter configurationService = new ConfigCenter("test", configCenterRepository);
        assertTrue(configurationService.isShadowRule("sharding_db"));
    }
    
    @Test
    public void assertIsNotShardingRule() {
        when(configCenterRepository.get("/test/config/schema/sharding_db/rule")).thenReturn(MASTER_SLAVE_RULE_YAML);
        ConfigCenter configurationService = new ConfigCenter("test", configCenterRepository);
        assertFalse(configurationService.isShardingRule("sharding_db"));
    }
    
    @Test
    public void assertLoadShardingRuleConfiguration() {
        when(configCenterRepository.get("/test/config/schema/sharding_db/rule")).thenReturn(SHARDING_RULE_YAML);
        ConfigCenter configurationService = new ConfigCenter("test", configCenterRepository);
        Collection<RuleConfiguration> actual = configurationService.loadRuleConfigurations("sharding_db");
        assertThat(actual.size(), is(1));
        ShardingRuleConfiguration actualShardingRuleConfiguration = (ShardingRuleConfiguration) actual.iterator().next();
        assertThat(actualShardingRuleConfiguration.getTableRuleConfigs().size(), is(1));
        assertThat(actualShardingRuleConfiguration.getTableRuleConfigs().iterator().next().getLogicTable(), is("t_order"));
    }
    
    @Test
    public void assertLoadMasterSlaveRuleConfiguration() {
        when(configCenterRepository.get("/test/config/schema/sharding_db/rule")).thenReturn(MASTER_SLAVE_RULE_YAML);
        ConfigCenter configurationService = new ConfigCenter("test", configCenterRepository);
        MasterSlaveRuleConfiguration actual = configurationService.loadMasterSlaveRuleConfiguration("sharding_db");
        assertThat(actual.getDataSources().iterator().next().getName(), is("ms_ds"));
    }
    
    @Test
    public void assertLoadEncryptRuleConfiguration() {
        when(configCenterRepository.get("/test/config/schema/sharding_db/rule")).thenReturn(ENCRYPT_RULE_YAML);
        ConfigCenter configurationService = new ConfigCenter("test", configCenterRepository);
        EncryptRuleConfiguration actual = configurationService.loadEncryptRuleConfiguration("sharding_db");
        assertThat(actual.getEncryptors().size(), is(1));
        Entry<String, EncryptorRuleConfiguration> entry = actual.getEncryptors().entrySet().iterator().next();
        assertThat(entry.getKey(), is("order_encryptor"));
        assertThat(entry.getValue().getType(), is("aes"));
        assertThat(entry.getValue().getProperties().get("aes.key.value").toString(), is("123456"));
    }
    
    @Test
    @Ignore
    // TODO fix shadow
    public void assertLoadShadowRuleConfiguration() {
        when(configCenterRepository.get("/test/config/schema/sharding_db/rule")).thenReturn(SHADOW_RULE_YAML);
        ConfigCenter configurationService = new ConfigCenter("test", configCenterRepository);
        ShadowRuleConfiguration actual = configurationService.loadShadowRuleConfiguration("sharding_db");
        assertThat(actual.getShadowMappings().get("ds"), is("shadow_ds"));
        assertThat(actual.getColumn(), is("shadow"));
    }
    
    @Test
    public void assertLoadAuthentication() {
        when(configCenterRepository.get("/test/config/authentication")).thenReturn(AUTHENTICATION_YAML);
        ConfigCenter configurationService = new ConfigCenter("test", configCenterRepository);
        Authentication actual = configurationService.loadAuthentication();
        assertThat(actual.getUsers().size(), is(2));
        assertThat(actual.getUsers().get("root1").getPassword(), is("root1"));
    }
    
    @Test
    public void assertLoadProperties() {
        when(configCenterRepository.get("/test/config/props")).thenReturn(PROPS_YAML);
        ConfigCenter configurationService = new ConfigCenter("test", configCenterRepository);
        Properties actual = configurationService.loadProperties();
        assertThat(actual.get(ConfigurationPropertyKey.SQL_SHOW.getKey()), is(Boolean.FALSE));
    }
    
    @Test
    public void assertGetAllShardingSchemaNames() {
        when(configCenterRepository.get("/test/config/schema")).thenReturn("sharding_db,masterslave_db");
        ConfigCenter configurationService = new ConfigCenter("test", configCenterRepository);
        Collection<String> actual = configurationService.getAllShardingSchemaNames();
        assertThat(actual.size(), is(2));
        assertThat(actual, hasItems("sharding_db"));
        assertThat(actual, hasItems("masterslave_db"));
    }
    
    @Test
    public void assertLoadDataSourceConfigurationsWithConnectionInitSqls() {
        when(configCenterRepository.get("/test/config/schema/sharding_db/datasource")).thenReturn(DATA_SOURCE_YAML_WITH_CONNECTION_INIT_SQLS);
        ConfigCenter configurationService = new ConfigCenter("test", configCenterRepository);
        Map<String, DataSourceConfiguration> actual = configurationService.loadDataSourceConfigurations("sharding_db");
        assertThat(actual.size(), is(2));
        assertDataSourceConfigurationWithConnectionInitSqls(actual.get("ds_0"), createDataSourceConfiguration(createDataSourceWithConnectionInitSqls("ds_0")));
        assertDataSourceConfigurationWithConnectionInitSqls(actual.get("ds_1"), createDataSourceConfiguration(createDataSourceWithConnectionInitSqls("ds_1")));
    }
    
    private DataSource createDataSourceWithConnectionInitSqls(final String name) {
        BasicDataSource result = new BasicDataSource();
        result.setDriverClassName("com.mysql.jdbc.Driver");
        result.setUrl("jdbc:mysql://localhost:3306/" + name);
        result.setUsername("root");
        result.setPassword("root");
        result.setConnectionInitSqls(Arrays.asList("set names utf8mb4;", "set names utf8;"));
        return result;
    }
    
    private void assertDataSourceConfigurationWithConnectionInitSqls(final DataSourceConfiguration actual, final DataSourceConfiguration expected) {
        assertThat(actual.getDataSourceClassName(), is(expected.getDataSourceClassName()));
        assertThat(actual.getProperties().get("url"), is(expected.getProperties().get("url")));
        assertThat(actual.getProperties().get("username"), is(expected.getProperties().get("username")));
        assertThat(actual.getProperties().get("password"), is(expected.getProperties().get("password")));
        assertThat(actual.getProperties().get("connectionInitSqls"), is(expected.getProperties().get("connectionInitSqls")));
    }

    private static String readYamlFileIntoString(final String fileName) {
        try {
            String yamlString = (new String(Files.readAllBytes(Paths.get(ClassLoader.getSystemResource(fileName).toURI()))))
                    .replaceAll("#.*\n", "");
            int indexOfFirstNewLineCharacter = yamlString.indexOf('\n');
            return yamlString.substring(indexOfFirstNewLineCharacter + 1);
        } catch (IOException | URISyntaxException ex) {
            log.error("Unable to open file {}", fileName);
            return null;
        }
    }
}
