package mes.config;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.RequestCacheConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import mes.domain.security.AjaxAwareLoginUrlAuthenticationEntryPoint;

import mes.domain.security.CustomAccessDeniedHandler;
import mes.domain.security.CustomAuthenticationFailureHandler;
import mes.domain.security.CustomAuthenticationManager;
import mes.domain.security.CustomAuthenticationSuccessHandler;


@Configuration
@ComponentScan("mes.domain.security")
public class SecurityConfiguration {
	
	@Autowired
	private CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler;

	@Autowired
	private CustomAuthenticationFailureHandler customAuthenticationFailureHandler;
	
		
	@Bean(name="authenticationManager")	
	CustomAuthenticationManager authenticationManager() {
		CustomAuthenticationManager authenticationManager = new CustomAuthenticationManager();
		return authenticationManager;
	}
	
    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.headers().frameOptions().sameOrigin(); 
        //http.csrf().disable();
        http.csrf().ignoringAntMatchers("/api/files/upload/**");
		http.csrf().ignoringAntMatchers("/api/sales/**");
		http.csrf().ignoringAntMatchers("/api/gene/**");
        
        http.authorizeRequests().mvcMatchers("/login","/logout", "/useridchk/**", "/Register/save").permitAll()
				.mvcMatchers("/api/sales/upload/**", "/api/gene/**").permitAll()  // 모든 사용자에게 허용 (임시)
				.mvcMatchers("/user-codes/**", "/user-auth/**").permitAll()
				.mvcMatchers("/authentication/**").permitAll()
				.mvcMatchers("/api/request/**").permitAll()
//				.mvcMatchers("/api/sales/upload/**").authenticated()  // 모든 인증된 사용자에게 허용 (임시)
        .mvcMatchers("/setup").hasAuthority("admin")		// hasRole -> hasAuthority로 수정
        .anyRequest().authenticated();

        http.formLogin()
        .loginPage("/login")
        .loginProcessingUrl("/postLogin")
        .successHandler(customAuthenticationSuccessHandler)
		.failureHandler(customAuthenticationFailureHandler)		
        .permitAll();
                
        http.logout().logoutUrl("/logout")
        .logoutSuccessUrl("/login")
        .invalidateHttpSession(true)
        .deleteCookies("mes21_jsessionid")
        .clearAuthentication(true)
        .permitAll();
        
        http.httpBasic().disable();
        http.exceptionHandling().accessDeniedHandler(new CustomAccessDeniedHandler()).authenticationEntryPoint(new AjaxAwareLoginUrlAuthenticationEntryPoint("/login"));
        

        
        return http.build();
    }

    /*
    @Bean
    WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().mvcMatchers("/intro", "/error", "/alive", "/api/das_device");
    }
    */
    
    @Bean
    @Order(0)
    SecurityFilterChain exceptResources(HttpSecurity http) throws Exception {
    	http.requestMatchers(matchers -> matchers.antMatchers("/resource/**","/img/**","/images/**", "/js/**","/css/**","/assets_mobile/css/**","/font/**","/robots.txt","/favicon.ico","/intro", "/error", "/alive", "/api/das_device"))
		.authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
		.requestCache(RequestCacheConfigurer::disable)
		.securityContext(AbstractHttpConfigurer::disable)
		.sessionManagement(AbstractHttpConfigurer::disable);   	
    	
    	http.headers().frameOptions().disable();
        return http.build();
    }     
    
}

