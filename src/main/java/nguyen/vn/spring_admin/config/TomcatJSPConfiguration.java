package nguyen.vn.spring_admin.config;


import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.http.MediaType;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class TomcatJSPConfiguration implements WebMvcConfigurer {

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer.defaultContentType(MediaType.TEXT_HTML);
    }

    @Bean
    public WebServerFactoryCustomizer<WebServerFactory>
    staticResourceCustomizer() {

        return factory -> {

            if (factory instanceof
                    TomcatServletWebServerFactory tomcatFactory) {

                tomcatFactory.addContextCustomizers(
                        context -> {
                            context.setRequestCharacterEncoding("UTF-8");
                            context.setResponseCharacterEncoding("UTF-8");
                            context.addLifecycleListener(
                                    new JSPStaticResourceConfigurer(context)
                            );
                        }
                );
            }
        };
    }

    @Bean
    public static org.springframework.beans.factory.config.BeanPostProcessor viewResolverContentTypePostProcessor() {
        return new org.springframework.beans.factory.config.BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof org.springframework.web.servlet.view.InternalResourceViewResolver resolver) {
                    resolver.setContentType("text/html;charset=UTF-8");
                }
                return bean;
            }
        };
    }
}