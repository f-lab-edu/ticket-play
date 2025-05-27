package com.flab.tiple.global.config;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @Import(RestDocsConfig.class)
 *   RestDocsConfig를 가져와서 이 클래스에 있는 @Bean을 현재 컨텍스트에 추가
 *   RestDocumentationResultHandler를 빈으로 등록하고, 요청/응답을 보기 좋게 정리하는 설정을 포함
 *   즉, 이 설정을 가져와서 restDocs 필드에 @Autowired로 주입받을 수 있게 해줌
 *
 * @AutoConfigureMockMvc(addFilters = false)
 *   MockMvc를 자동으로 설정 -> 필터 비활성화
 *
 * @AutoConfigureRestDocs
 *    Spring REST Docs를 테스트 환경에서 사용할 수 있도록 설정 -> 내부적으로 MockMvcRestDocumentationConfigurer를 설정해서 REST Docs의 기본 설정을 적용
 *
 * @ExtendWith(RestDocumentationExtension.class)
 *    RestDocumentationExtension을 확장해서 Spring REST Docs가 테스트 컨텍스트에서 사용할 수 있도록 환경을 자동 구성
 *
 */
@Import(RestDocsConfig.class)
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureRestDocs
@ExtendWith(RestDocumentationExtension.class)
public abstract class AbstractRestDocs {

    @Autowired
    protected RestDocumentationResultHandler restDocs;

    @Autowired
    protected ObjectMapper objectMapper;

    protected MockMvc mockMvc;

    /**
     * @BeforeEach → 각 테스트 실행 전에 실행되는 메서드. -> MockMvc를 생성하면서 REST Docs를 적용하는 설정을 추가
     *
     */
    @BeforeEach
    void setUp(
            final WebApplicationContext context,
            final RestDocumentationContextProvider restDocumentation) {
        //WebApplicationContext를 기반으로 MockMvc를 생성
        //즉, Spring 애플리케이션 컨텍스트 기반의 통합 테스트를 수행할 수 있도록 함.
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
                //RestDocumentationContextProvider를 사용해서 Spring REST Docs를 적용하는 MockMvc 설정을 추가
                .apply(documentationConfiguration(restDocumentation))
                //모든 요청의 요청/응답 정보를 출력하도록 설정
                .alwaysDo(MockMvcResultHandlers.print())
                //모든 요청에 대해 REST Docs 문서화를 수행하도록 설정. -> @Import(RestDocsConfig.class)에서 가져온 RestDocumentationResultHandler를 적용
                .alwaysDo(restDocs)
                //응답의 문자 인코딩을 UTF-8로 설정해서 한글이 깨지는 걸 방지.
                .addFilters(new CharacterEncodingFilter("UTF-8", true))
                .build();
    }
}