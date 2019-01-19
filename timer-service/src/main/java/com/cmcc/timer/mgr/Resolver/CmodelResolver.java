package com.cmcc.timer.mgr.Resolver;

import java.io.IOException;
import java.lang.annotation.Annotation;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.Conventions;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.cmcc.timer.mgr.ExceptionHandler.MgrExceptionFactory;
import com.cmcc.timer.mgr.annotation.Cmodel;
import com.cmcc.timer.mgr.annotation.Env;
import com.cmcc.timer.mgr.annotation.JsonTimeDeserializer;
import com.cmcc.timer.mgr.annotation.JsonTimeSerializer;
import com.cmcc.timer.mgr.exception.MgrBaseException;
import com.cmcc.timer.mgr.util.CacheUtil;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;

/**
 * 仿照RequestResponseBodyMethodProcessor从attribute字段jason反序化绑定到
 * 参数上,和logFilter配合使用
 * @see {@linkplain LogFilter}
 * @author silver
 *
 */
@Component
public class CmodelResolver implements HandlerMethodArgumentResolver{

    @Autowired
    private MgrExceptionFactory mgrExceptionFactory;
    
    @Autowired
    private CacheUtil cacheUtil;
    
    @Value("${env}")
    private String env;
    
    
    private ObjectMapper mapper = new ObjectMapper();  //加入对时间的校验，datedimeforamt的设置
    
    @Autowired
    public CmodelResolver(){
        addMapperIntrospector(mapper);
    }
    
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        // TODO Auto-generated method stub
        return parameter.hasParameterAnnotation(Cmodel.class);
    }
    /**
     * 这里仅简单调用jakson反序列化对象，因参数可见性问题，无法继承RequestResponseBodyMethodProcessor，
     * 更多特性无法使用到，有需要后续扩展deserialize方法
     */
    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        // TODO Auto-generated method stub
        //check for authority
        checkAuthority(parameter);
        HttpServletRequest request = webRequest
                .getNativeRequest(HttpServletRequest.class);
        String cModelString = (String) request.getAttribute("cModel");
        Object target = deserialize(parameter, cModelString); 
        String name = Conventions.getVariableNameForParameter(parameter);
        WebDataBinder binder = binderFactory.createBinder(webRequest, target, name);
        validateIfApplicable(binder, parameter);
        if (binder.getBindingResult().hasErrors() && isBindExceptionRequired(binder, parameter)) {
            throw new MethodArgumentNotValidException(parameter, binder.getBindingResult());
        }
        mavContainer.addAttribute(BindingResult.MODEL_KEY_PREFIX + name, binder.getBindingResult());
        return target;
    }
    
    private void checkAuthority(MethodParameter parameter){
        Env ev = parameter.getMethodAnnotation(Env.class);
        if(ev != null && !cacheUtil.check(ev.values(), env)){
            throw mgrExceptionFactory.createException("100000000");
        }
    }
    
    private Object deserialize(MethodParameter parameter, String cModelString)
            throws IOException, JsonParseException, JsonMappingException {
            Object target = mapper.readValue(cModelString,parameter.getParameterType());
            return target;
    }
    
    public   void addMapperIntrospector(ObjectMapper mapper){
        JacksonAnnotationIntrospector jacksonAnnotationIntrospector = new JacksonAnnotationIntrospector(){

            @Override
            public Object findDeserializer(Annotated a) {
                // TODO Auto-generated method stub
                if(a instanceof AnnotatedMethod){
                    JsonTimeDeserializer desAnnotation = a.getAnnotated().getAnnotation(JsonTimeDeserializer.class);
                    if(desAnnotation != null){
                        //后期这个地方写成factory模式，不用每次new出一个新的，提高整体效率
                        JsonDateDeserializeResolver jsonDateDeserializeResolver = cacheUtil.getJsonDateDeserializeResolver(desAnnotation.pattern());
                        jsonDateDeserializeResolver.setMgrExceptionFactory(mgrExceptionFactory);
                        return jsonDateDeserializeResolver;
                    }
                }
                return super.findDeserializer(a);
            }
            public Object findSerializer(Annotated a){
                if(a instanceof AnnotatedMethod){
                    JsonTimeSerializer sAnnotation = a.getAnnotated().getAnnotation(JsonTimeSerializer.class);
                    if(sAnnotation != null){
                        //后期这个地方写成factory模式，不用每次new出一个新的，提高整体效率
                        return cacheUtil.getJsonDateSerializeResolver(sAnnotation.pattern());
                    }
                }
                return super.findSerializer(a);
            }
            
            
        };
        mapper.setAnnotationIntrospector(jacksonAnnotationIntrospector);
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    }
    
    
    protected void validateIfApplicable(WebDataBinder binder, MethodParameter parameter) {
        Annotation[] annotations = parameter.getParameterAnnotations();
        for (Annotation ann : annotations) {
            Validated validatedAnn = AnnotationUtils.getAnnotation(ann, Validated.class);
            if (validatedAnn != null || ann.annotationType().getSimpleName().startsWith("Valid")) {
                Object hints = (validatedAnn != null ? validatedAnn.value() : AnnotationUtils.getValue(ann));
                Object[] validationHints = (hints instanceof Object[] ? (Object[]) hints : new Object[] {hints});
                binder.validate(validationHints);
                break;
            }
        }
    }
    
    protected boolean isBindExceptionRequired(WebDataBinder binder, MethodParameter parameter) {
        int i = parameter.getParameterIndex();
        Class<?>[] paramTypes = parameter.getMethod().getParameterTypes();
        boolean hasBindingResult = (paramTypes.length > (i + 1) && Errors.class.isAssignableFrom(paramTypes[i + 1]));
        return !hasBindingResult;
    }
    
    
    

    
    
    

}
