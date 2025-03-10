package com.flab.tiple.global.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.restdocs.operation.preprocess.Preprocessors;

/**
 * @TestConfiguration: @Configuration과 유사하지만, 테스트에서만 사용하도록 설정된 버전.
 * 만약 @Configuration을 사용하면 애플리케이션 전체에서 이 설정이 적용될 수 있음.
 * 하지만, @TestConfiguration을 사용하면 테스트 코드에서만 적용되도록 제한할 수 있음.
 *
 */
@TestConfiguration
public class RestDocsConfig {

    /**
     * 빈으로 Spring 컨테이너에서 관리할 RestDocumentationResultHandler 객체를 생성
     * RestDocumentationResultHandler는 Spring REST Docs에서 요청(Request)과 응답(Response)을 문서화하는 역할
     * 이 Bean을 MockMvc 테스트에서 사용해서 API 문서를 자동으로 생성할 수 있음.
     */
    @Bean
    public RestDocumentationResultHandler write() {

        /**
         * Spring REST Docs에서 API 문서를 생성하는 역할
         * Preprocessors: Spring REST Docs에서 API 요청(Request)과 응답(Response)을 미리 가공(Preprocess)하는 역할(데이터를 보기 좋게 정렬)
         */
        return MockMvcRestDocumentation.document(
                "{class-name}/{method-name}",
                Preprocessors.preprocessRequest(Preprocessors.prettyPrint()),
                Preprocessors.preprocessResponse(Preprocessors.prettyPrint())
        );

    }
}