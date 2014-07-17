/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.opentox.jaqpot3.qsar.util;

import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.dmg.pmml.Aggregate;
import org.dmg.pmml.Apply;
import org.dmg.pmml.Constant;
import org.dmg.pmml.DataType;
import org.dmg.pmml.DerivedField;
import org.dmg.pmml.Discretize;
import org.dmg.pmml.Expression;
import org.dmg.pmml.FieldColumnPair;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.FieldRef;
import org.dmg.pmml.InvalidValueTreatmentMethodType;
import org.dmg.pmml.MapValues;
import org.dmg.pmml.NormContinuous;
import org.dmg.pmml.NormDiscrete;
import org.jpmml.evaluator.DiscretizationUtil;
import org.jpmml.evaluator.EvaluationContext;
import static org.jpmml.evaluator.ExpressionUtil.evaluate;
import static org.jpmml.evaluator.ExpressionUtil.evaluateAggregate;
import static org.jpmml.evaluator.ExpressionUtil.evaluateDiscretize;
import static org.jpmml.evaluator.ExpressionUtil.evaluateFieldRef;
import static org.jpmml.evaluator.ExpressionUtil.evaluateMapValues;
import static org.jpmml.evaluator.ExpressionUtil.evaluateNormContinuous;
import static org.jpmml.evaluator.ExpressionUtil.evaluateNormDiscrete;
import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.FieldValueUtil;
import org.jpmml.evaluator.InvalidResultException;
import org.jpmml.evaluator.NormalizationUtil;
import org.jpmml.evaluator.TypeCheckException;
import org.jpmml.evaluator.TypeUtil;
import org.jpmml.manager.UnsupportedFeatureException;
import org.opentox.jaqpot3.qsar.util.FunctionUtil;
/**
 *
 * @author lab
 */
public class ExpressionUtilExtended {
       
        static
	public FieldValue evaluate(FieldName name, EvaluationContext context){
		Map.Entry<FieldName, FieldValue> entry = context.getFieldEntry(name);
		if(entry == null){
			DerivedField derivedField = context.resolveDerivedField(name);
			if(derivedField == null){
				return null;
			}

			FieldValue value = evaluate(derivedField, context);

			// Make the calculated value available for re-use
			context.declare(name, value);

			return value;
		}

		return entry.getValue();
	}

	static
	public FieldValue evaluate(DerivedField derivedField, EvaluationContext context){
		FieldValue value = evaluate(derivedField.getExpression(), context,null);

		return FieldValueUtil.refine(derivedField, value);
	}
        
        static
	public FieldValue evaluate(DerivedField derivedField, EvaluationContext context,
                                           Map<String,Double> replaceURIs){
		FieldValue value = evaluate(derivedField.getExpression(), context,replaceURIs);

		return FieldValueUtil.refine(derivedField, value);
	}

	static
	public FieldValue evaluate(Expression expression, EvaluationContext context,
                                   Map<String,Double> replaceURIs){

		if(expression instanceof Constant){
			return evaluateConstant((Constant)expression, context,replaceURIs);
		} else

		if(expression instanceof FieldRef){
			return evaluateFieldRef((FieldRef)expression, context);
		} else

		if(expression instanceof NormContinuous){
			return evaluateNormContinuous((NormContinuous)expression, context);
		} else

		if(expression instanceof NormDiscrete){
			return evaluateNormDiscrete((NormDiscrete)expression, context);
		} else

		if(expression instanceof Discretize){
			return evaluateDiscretize((Discretize)expression, context);
		} else

		if(expression instanceof MapValues){
			return evaluateMapValues((MapValues)expression, context);
		} else

		if(expression instanceof Apply){
			return evaluateApply((Apply)expression, context,replaceURIs);
		} else

		if(expression instanceof Aggregate){
			return evaluateAggregate((Aggregate)expression, context);
		}

		throw new UnsupportedFeatureException(expression);
	}

	static
	public FieldValue evaluateConstant(Constant constant, EvaluationContext context,
                                           Map<String,Double> replaceURIs){
		String value = constant.getValue();
                if (!replaceURIs.isEmpty()) {
                    if (replaceURIs.containsKey(value)) {
                        value = replaceURIs.get(value).toString();
                    }
                }
		DataType dataType = constant.getDataType();
		if(dataType == null){
			dataType = TypeUtil.getConstantDataType(value);
		}

		return FieldValueUtil.create(dataType, null, value);
	}

	static
	public FieldValue evaluateFieldRef(FieldRef fieldRef, EvaluationContext context){
		FieldValue value = evaluate(fieldRef.getField(), context);
		if(value == null){
			return FieldValueUtil.create(fieldRef.getMapMissingTo());
		}

		return value;
	}

	static
	public FieldValue evaluateNormContinuous(NormContinuous normContinuous, EvaluationContext context){
		FieldValue value = evaluate(normContinuous.getField(), context);
		if(value == null){
			return FieldValueUtil.create(normContinuous.getMapMissingTo());
		}

		return NormalizationUtil.normalize(normContinuous, value);
	}

	static
	public FieldValue evaluateNormDiscrete(NormDiscrete normDiscrete, EvaluationContext context){
		FieldValue value = evaluate(normDiscrete.getField(), context);
		if(value == null){
			return FieldValueUtil.create(normDiscrete.getMapMissingTo());
		}

		NormDiscrete.Method method = normDiscrete.getMethod();
		switch(method){
			case INDICATOR:
				{
					boolean equals = value.equalsString(normDiscrete.getValue());

					return FieldValueUtil.create(equals ? 1d : 0d);
				}
			default:
				throw new UnsupportedFeatureException(normDiscrete, method);
		}
	}

	static
	public FieldValue evaluateDiscretize(Discretize discretize, EvaluationContext context){
		FieldValue value = evaluate(discretize.getField(), context);
		if(value == null){
			return FieldValueUtil.create(discretize.getDataType(), null, discretize.getMapMissingTo());
		}

		return DiscretizationUtil.discretize(discretize, value);
	}

	static
	public FieldValue evaluateMapValues(MapValues mapValues, EvaluationContext context){
		Map<String, FieldValue> values = Maps.newLinkedHashMap();

		List<FieldColumnPair> fieldColumnPairs = mapValues.getFieldColumnPairs();
		for(FieldColumnPair fieldColumnPair : fieldColumnPairs){
			FieldValue value = evaluate(fieldColumnPair.getField(), context);
			if(value == null){
				return FieldValueUtil.create(mapValues.getDataType(), null, mapValues.getMapMissingTo());
			}

			values.put(fieldColumnPair.getColumn(), value);
		}

		return DiscretizationUtil.mapValue(mapValues, values);
	}

	static
	public FieldValue evaluateApply(Apply apply, EvaluationContext context,
                                        Map<String,Double> replaceURIs){
		String mapMissingTo = apply.getMapMissingTo();

		List<FieldValue> values = Lists.newArrayList();

		List<Expression> arguments = apply.getExpressions();
		for(Expression argument : arguments){
			FieldValue value = evaluate(argument, context,replaceURIs);

			// "If a mapMissingTo value is specified and any of the input values of the function are missing, then the function is not applied at all and the mapMissingTo value is returned instead"
			if(value == null && mapMissingTo != null){
				return FieldValueUtil.create(mapMissingTo);
			}

			values.add(value);
		}

		String defaultValue = apply.getDefaultValue();

		FieldValue result;

		try {
			result = org.opentox.jaqpot3.qsar.util.FunctionUtil.evaluate(apply, values, context);
		} catch(InvalidResultException ire){
			InvalidValueTreatmentMethodType invalidValueTreatmentMethod = apply.getInvalidValueTreatment();

			switch(invalidValueTreatmentMethod){
				case RETURN_INVALID:
					throw new InvalidResultException(apply);
				case AS_IS:
					// Re-throw the given InvalidResultException instance
					throw ire;
				case AS_MISSING:
					return FieldValueUtil.create(defaultValue);
				default:
					throw new UnsupportedFeatureException(apply, invalidValueTreatmentMethod);
			}
		}

		// "If a defaultValue value is specified and the function produced a missing value, then the defaultValue is returned"
		if(result == null && defaultValue != null){
			return FieldValueUtil.create(defaultValue);
		}

		return result;
	}

	@SuppressWarnings (
		value = {"rawtypes", "unchecked"}
	)
	static
	public FieldValue evaluateAggregate(Aggregate aggregate, EvaluationContext context){
		FieldValue value = evaluate(aggregate.getField(), context);

		Collection<?> values;

		// The JPMML library operates with single records, so it's impossible to implement "proper" aggregation over multiple records
		// It is assumed that the aggregation has been performed by application developer beforehand
		try {
			values = (Collection<?>)FieldValueUtil.getValue(value);
		} catch(ClassCastException cce){
			throw new TypeCheckException(Collection.class, value);
		}

		FieldName groupName = aggregate.getGroupField();
		if(groupName != null){
			FieldValue groupValue = evaluate(groupName, context);

			// Ensure that the group value is a simple type, not a collection type
			TypeUtil.getDataType(FieldValueUtil.getValue(groupValue));
		}

		// Remove missing values
		values = Lists.newArrayList(Iterables.filter(values, Predicates.notNull()));

		Aggregate.Function function = aggregate.getFunction();
		switch(function){
			case COUNT:
				return FieldValueUtil.create(values.size());
			case SUM:
				return org.opentox.jaqpot3.qsar.util.FunctionUtil.evaluate(new Apply("sum"), createValues(values), context);
			case AVERAGE:
				return org.opentox.jaqpot3.qsar.util.FunctionUtil.evaluate(new Apply("avg"), createValues(values), context);
			case MIN:
				return FieldValueUtil.create(Collections.min((List<Comparable>)values));
			case MAX:
				return FieldValueUtil.create(Collections.max((List<Comparable>)values));
			default:
				throw new UnsupportedFeatureException(aggregate, function);
		}
	}

	static
	private List<FieldValue> createValues(Collection<?> values){
		Function<Object, FieldValue> function = new Function<Object, FieldValue>(){

			@Override
			public FieldValue apply(Object value){
				return FieldValueUtil.create(value);
			}
		};

		return Lists.newArrayList(Iterables.transform(values, function));
	}
}
