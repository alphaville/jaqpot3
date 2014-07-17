/*
 * Copyright (c) 2013 Villu Ruusmann
 *
 * This file is part of JPMML-Evaluator
 *
 * JPMML-Evaluator is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * JPMML-Evaluator is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with JPMML-Evaluator.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.opentox.jaqpot3.qsar.util;

import java.util.List;

import org.dmg.pmml.Apply;
import org.dmg.pmml.DefineFunction;
import org.dmg.pmml.Expression;
import org.dmg.pmml.ParameterField;
import org.jpmml.evaluator.DefineFunctionEvaluationContext;
import org.jpmml.evaluator.EvaluationContext;
import org.jpmml.evaluator.ExpressionUtil;
import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.FieldValueUtil;
import org.jpmml.evaluator.Function;
import org.jpmml.evaluator.FunctionException;
import org.jpmml.manager.InvalidFeatureException;
import org.jpmml.manager.UnsupportedFeatureException;

public class FunctionUtil {

	private FunctionUtil(){
	}

	static
	public FieldValue evaluate(Apply apply, List<FieldValue> values, EvaluationContext context){
		String name = apply.getFunction();

		Function function = FunctionRegistry.getFunction(name);
		if(function != null){
			return function.evaluate(values);
		}

		DefineFunction defineFunction = context.resolveFunction(name);
		if(defineFunction != null){
			return evaluate(defineFunction, values, context);
		}

		throw new UnsupportedFeatureException(apply);
	}

	static
	public FieldValue evaluate(DefineFunction defineFunction, List<FieldValue> values, EvaluationContext context){
		List<ParameterField> parameterFields = defineFunction.getParameterFields();

		if(parameterFields.size() < 1){
			throw new InvalidFeatureException(defineFunction);
		} // End if

		if(parameterFields.size() != values.size()){
			throw new FunctionException(defineFunction.getName(), "Expected " + parameterFields.size() + " arguments, but got " + values.size() + " arguments");
		}

		DefineFunctionEvaluationContext functionContext = new DefineFunctionEvaluationContext(context);

		for(int i = 0; i < parameterFields.size(); i++){
			ParameterField parameterField = parameterFields.get(i);

			FieldValue value = FieldValueUtil.refine(parameterField, values.get(i));

			functionContext.declare(parameterField.getName(), value);
		}

		Expression expression = defineFunction.getExpression();
		if(expression == null){
			throw new InvalidFeatureException(defineFunction);
		}

		FieldValue result = ExpressionUtil.evaluate(expression, functionContext);

		return FieldValueUtil.refine(defineFunction.getDataType(), defineFunction.getOptype(), result);
	}
}