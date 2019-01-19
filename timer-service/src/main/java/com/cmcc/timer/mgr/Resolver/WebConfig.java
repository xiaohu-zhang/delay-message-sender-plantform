package com.cmcc.timer.mgr.Resolver;

import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;
import org.springframework.util.InvalidMimeTypeException;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import com.fasterxml.jackson.databind.SerializationFeature;


@Configuration
@EnableWebMvc
@ComponentScan(basePackageClasses = { WebConfig.class })
public class WebConfig extends WebMvcConfigurerAdapter {
    @Autowired
    private CmodelResolver cmodelResolver;
    
    @Override
    public void configureMessageConverters (List<HttpMessageConverter<?>> converters) {
        // TODO Auto-generated method stub
        StringHttpMessageConverter stringHttpMessageConverter = new StringHttpMessageConverter(Charset.forName("UTF-8"));
        stringHttpMessageConverter.setSupportedMediaTypes(Arrays.asList(parseMediaType(StringHttpMessageConverter.class,"text/plain;charset=UTF-8")));
        converters.add(stringHttpMessageConverter);
        ByteArrayHttpMessageConverter byteArrayHttpMessageConverter = new ByteArrayHttpMessageConverter();
        ResourceHttpMessageConverter resourceHttpMessageConverter = new ResourceHttpMessageConverter();
        @SuppressWarnings("rawtypes")
        SourceHttpMessageConverter SourceHttpMessageConverter = new SourceHttpMessageConverter();
        AllEncompassingFormHttpMessageConverter allEncompassingFormHttpMessageConverter = new AllEncompassingFormHttpMessageConverter();
        Jaxb2RootElementHttpMessageConverter jaxb2RootElementHttpMessageConverter= new Jaxb2RootElementHttpMessageConverter();
        MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter = new MappingJackson2HttpMessageConverter();
        mappingJackson2HttpMessageConverter.getObjectMapper().configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mappingJackson2HttpMessageConverter.getObjectMapper().setDateFormat(new SimpleDateFormat("yyyyMMdd HH:mm:ss"));
        cmodelResolver.addMapperIntrospector(mappingJackson2HttpMessageConverter.getObjectMapper());
        converters.add(mappingJackson2HttpMessageConverter);
        converters.add(jaxb2RootElementHttpMessageConverter);
        converters.add(allEncompassingFormHttpMessageConverter);
        converters.add(SourceHttpMessageConverter);
        converters.add(resourceHttpMessageConverter);
        converters.add(byteArrayHttpMessageConverter);
        
    }
    
    public static MediaType parseMediaType(Class<? super StringHttpMessageConverter> converterClass,String mediaType) {
        MimeType type;
        try {
            type = MimeTypeUtils.parseMimeType(mediaType);
        } catch (InvalidMimeTypeException ex) {
            throw new RuntimeException(ex);
        }
        try {
            Map<String,String> Parameters = new HashMap<>();
            Parameters.put("q", "0.9");
            for(Entry<String, String> e:type.getParameters().entrySet()){
                Parameters.put(e.getKey(), e.getValue());
            }
            return new MediaType(type.getType(), type.getSubtype(), Parameters);
        } catch (IllegalArgumentException ex) {
            throw new InvalidMediaTypeException(mediaType, ex.getMessage());
        }
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        argumentResolvers.add(cmodelResolver);
    }
    
}
