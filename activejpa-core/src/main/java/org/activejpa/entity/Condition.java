/**
 * 
 */
package org.activejpa.entity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.activejpa.util.ConvertUtil;

/**
 * @author ganeshs
 *
 */
public class Condition extends AbstractConstruct {

	/**
	 * @author ganeshs
	 *
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	public enum Operator {
		eq("=") {
			@Override
			protected Predicate createPredicate(CriteriaBuilder builder, Path path, Expression... parameter) {
				return builder.equal(path, parameter[0]);
			} 
		},
		ne("!=") {
			@Override
			protected Predicate createPredicate(CriteriaBuilder builder, Path path, Expression... parameter) {
				return builder.notEqual(path, parameter[0]);
			}
		},
		le("<=") {
			@Override
			protected Predicate createPredicate(CriteriaBuilder builder, Path path, Expression... parameter) {
				return builder.lessThanOrEqualTo(path, parameter[0]);
			}
		},
		lt("<") {
			@Override
			protected Predicate createPredicate(CriteriaBuilder builder, Path path, Expression... parameter) {
				return builder.lessThan(path, parameter[0]);
			}
		},
		ge(">=") {
			@Override
			protected Predicate createPredicate(CriteriaBuilder builder, Path path, Expression... parameter) {
				return builder.greaterThanOrEqualTo(path, parameter[0]);
			}
		}, 
		gt(">") {
			@Override
			protected Predicate createPredicate(CriteriaBuilder builder, Path path, Expression... parameter) {
				return builder.greaterThan(path, parameter[0]);
			}
		},
		between("between") {
			@Override
			public String constructCondition(String name, Object value) {
				return name + " between :from" + name + " and :to" + name;
			}
			
			@Override
			public Predicate constructCondition(CriteriaBuilder builder, Path path, String name) {
				name = cleanName(name);
				return createPredicate(builder, path, builder.parameter(path.getJavaType(), "from" + name), builder.parameter(path.getJavaType(), "to" + name));
			}
			
			@Override
			public void setParameters(Query query, String name, Object value, Class<?> paramType) {
				name = cleanName(name);
				Object[] values = null;
				if (value instanceof Object[]) {
					values = (Object[]) value;
				}
				if (values == null || values.length != 2) {
					throw new IllegalArgumentException("Value - " + value + " should be an array of size 2");
				}
				super.setParameters(query, "from" + name, values[0], paramType);
				super.setParameters(query, "to" + name, values[1], paramType);
			}
			
			@Override
			protected Predicate createPredicate(CriteriaBuilder builder, Path path, Expression... parameter) {
				return builder.between(path, parameter[0], parameter[1]);
			}
		}, 
		in("in") {
			@Override
			protected Predicate createPredicate(CriteriaBuilder builder, Path path, Expression... parameter) {
				return path.in(parameter[0]);
			}
			
			@Override
			public void setParameters(Query query, String name, Object value, Class<?> paramType) {
				name = cleanName(name);
				if (value instanceof Object[]) {
					value = Arrays.asList((Object[]) value);
				}
				List list = new ArrayList();
				for (Object val : (List) value) {
					list.add(ConvertUtil.convert(val, paramType));
				}
				query.setParameter(name, list);
			}
			
			@Override
			public Predicate constructCondition(CriteriaBuilder builder, Path path, String name) {
				name = cleanName(name);
				return createPredicate(builder, path, builder.parameter(Collection.class, name));
			}
		},
		like("like") {
			@Override
			protected Predicate createPredicate(CriteriaBuilder builder, Path path, Expression... parameter) {
				return builder.like(path, parameter[0]);
			}
		};
		
		private String operator;
		
		private Operator(String operator) {
			this.operator = operator;
		}
		
		public String constructCondition(String name, Object value) {
			return name + " " + operator + " :" + name;
		}
		
		public void setParameters(Query query, String name, Object value, Class<?> paramType) {
			name = cleanName(name);
			value = ConvertUtil.convert(value, paramType);
			query.setParameter(name, value);
		}
		
		public Predicate constructCondition(CriteriaBuilder builder, Path path, String name) {
			return createPredicate(builder, path, builder.parameter(path.getJavaType(), cleanName(name)));
		}
		
		protected abstract Predicate createPredicate(CriteriaBuilder builder, Path path, Expression... parameter);
		
		/**
		 * Cleans the name to align with the jpa query language constructs
		 *  
		 * @param name
		 * @return
		 */
		protected String cleanName(String name) {
			return name.replace(".", "0_");
		}
	}
	
	private String name;
	
	private Object value;
	
	private Operator operator;
	
	private Path<?> path;
	
	/**
	 * Default constructor
	 */
	public Condition() {
	}
	
	/**
	 * @param name
	 * @param operator
	 * @param value
	 */
	public Condition(String name, Operator operator, Object value) {
		this.name = name;
		this.value = value;
		this.operator = operator;
	}
	
	/**
	 * @param name
	 * @param value
	 */
	public Condition(String name, Object value) {
		this(name, Operator.eq, value);
	}
	
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the value
	 */
	public Object getValue() {
		return value;
	}

	/**
	 * @param value the value to set
	 */
	public void setValue(Object value) {
		this.value = value;
	}

	/**
	 * @return the operator
	 */
	public Operator getOperator() {
		return operator;
	}

	/**
	 * @param operator the operator to set
	 */
	public void setOperator(Operator operator) {
		this.operator = operator;
	}

	public String constructQuery() {
		return operator.constructCondition(name, value);
	}
	
	public <T extends Model> Predicate constructQuery(CriteriaBuilder builder, Root<T> root) {
		path = getPath(root, name);
		return operator.constructCondition(builder, path, name);
	}
	
	public void setParameters(Query query, Object value) {
		operator.setParameters(query, name, value, path.getJavaType());
	}

	/**
	 * @return the path
	 */
	Path<?> getPath() {
		return path;
	}

	/**
	 * @param path the path to set
	 */
	void setPath(Path<?> path) {
		this.path = path;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result
				+ ((operator == null) ? 0 : operator.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Condition other = (Condition) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (operator != other.operator)
			return false;
		return true;
	}
}
