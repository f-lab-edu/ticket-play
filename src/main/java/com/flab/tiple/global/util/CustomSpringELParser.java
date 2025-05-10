package com.flab.tiple.global.util;

import java.util.HashMap;

import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

public class CustomSpringELParser {
	// 싱글톤 인스턴스로 파서 공유
	private static final SpelExpressionParser PARSER = new SpelExpressionParser();

	// 스레드 로컬을 사용하여 컨텍스트 재사용
	private static final ThreadLocal<StandardEvaluationContext> CONTEXT_HOLDER =
		ThreadLocal.withInitial(StandardEvaluationContext::new);

	public static Object getDynamicValue(String[] parameterNames, Object[] args, String key) {
		StandardEvaluationContext context = CONTEXT_HOLDER.get();

		// 컨텍스트 초기화 (이전 변수들 제거)
		context.setVariables(new HashMap<>());

		// 변수 설정
		for (int i = 0; i < parameterNames.length; i++) {
			context.setVariable(parameterNames[i], args[i]);
		}

		// 표현식 평가
		return PARSER.parseExpression(key).getValue(context, Object.class);
	}

	// 애플리케이션 종료 시 호출할 정리 메소드
	public static void cleanup() {
		CONTEXT_HOLDER.remove();
	}
}