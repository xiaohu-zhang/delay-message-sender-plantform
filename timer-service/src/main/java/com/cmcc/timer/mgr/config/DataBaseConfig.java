package com.cmcc.timer.mgr.config;

import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.mapper.MapperScannerConfigurer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;

@Configuration
public class DataBaseConfig {

    @Value("${redis.password}")
    private String redisPassword;
    
    @Bean
    public BasicDataSource getDateSource(DataBasePerperties dataBasePerperties){
        BasicDataSource dataSource = new BasicDataSource();
        propertiesSet(dataSource,dataBasePerperties);
        return dataSource;
    }
    
    @Bean(name="sqlSessionFactory")
    public SqlSessionFactory getSqlSessionFactoryBean(BasicDataSource dataSource) throws Exception{
        SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
        sqlSessionFactoryBean.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath*:sqlmapper/*Mapper.xml"));
        sqlSessionFactoryBean.setDataSource(dataSource);
        return sqlSessionFactoryBean.getObject();
    }
    
    
    @Bean
    public static MapperScannerConfigurer mapperScannerConfigurer() {
        MapperScannerConfigurer mapperScannerConfigurer = new MapperScannerConfigurer();
        mapperScannerConfigurer.setSqlSessionFactoryBeanName("sqlSessionFactory");
        mapperScannerConfigurer.setBasePackage("com.cmcc.timer.mgr.dao");
        return mapperScannerConfigurer;
    }

    private void propertiesSet(BasicDataSource dataSource,DataBasePerperties dataBasePerperties) {
        dataSource.setDriverClassName("com.mysql.jdbc.Driver");
        dataSource.setUsername(dataBasePerperties.getUsername());
        dataSource.setUrl(dataBasePerperties.getUrl());
        dataSource.setPassword(dataBasePerperties.getPassword());
        dataSource.setMaxActive(dataBasePerperties.getMaxActive());
        dataSource.setMaxIdle(dataBasePerperties.getMaxIdle());
        dataSource.setMaxWait(dataBasePerperties.getMaxWait());
        dataSource.setDefaultAutoCommit(true);
        dataSource.setMinEvictableIdleTimeMillis(3600000l);
        dataSource.setTimeBetweenEvictionRunsMillis(600000l);
        dataSource.setNumTestsPerEvictionRun(-1);
    }
    @Bean
    public GenericObjectPoolConfig redisConfig(){
        GenericObjectPoolConfig g = new GenericObjectPoolConfig();
        g.setMaxTotal(20);
        g.setMinIdle(5);
        g.setMaxIdle(5);
        g.setMaxWaitMillis(2000);
        g.setMinEvictableIdleTimeMillis(3600000l);
        g.setTimeBetweenEvictionRunsMillis(600000l);
        g.setNumTestsPerEvictionRun(-1);
        return g;
    }
    /** */
    @Bean
    public JedisCluster redisCluser(RedisClusterNodes nodes, GenericObjectPoolConfig config) {
        Set<HostAndPort> rr =nodes.getNodes().stream().map(HostAndPort::parseString).collect(Collectors.toSet());
        return new JedisCluster(rr,1000, 1000,1,redisPassword, config);
    }
    
//  @Bean
//  @Order(value=3)
//  public MapperFactoryBean getMapperFactoryBean(SqlSessionFactory sqlSessionFactory) throws Exception{
//      MapperFactoryBean MapperFactoryBean = new MapperFactoryBean();
//      MapperFactoryBean.setSqlSessionFactory(sqlSessionFactory);
//      MapperFactoryBean.setMapperInterface(mapperInterface);
//      return MapperFactoryBean;
//  }


    
    
}
