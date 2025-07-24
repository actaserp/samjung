package mes.config;


import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@Configuration
public class WebMvcConfig implements WebMvcConfigurer{

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //registry.addInterceptor(new GuiHttpInterceptor()).addPathPatterns("/gui/*");
        //registry.addInterceptor(new ApiHttpInterceptor()).addPathPatterns("/Api/*");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry){
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:8060") // 모든 도메인 허용
                .allowedMethods("GET", "POST", "PUT", "DELETE", "HEAD")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/editorFile/**")
                .addResourceLocations("file:///c:/temp/editorFile/");
        registry.addResourceHandler("/baljuFile/**")
            .addResourceLocations("file:///C:/Temp/mes21/외주발주서/");

    }

}
