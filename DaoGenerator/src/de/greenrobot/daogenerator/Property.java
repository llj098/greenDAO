/*
 * Copyright (C) 2011 Markus Junginger, greenrobot (http://greenrobot.de)
 *
 * This file is part of greenDAO Generator.
 * 
 * greenDAO Generator is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * greenDAO Generator is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with greenDAO Generator.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.greenrobot.daogenerator;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Model class for an entity's property: a Java property mapped to a data base
 * column.
 */
public class Property {

	public static class PropertyBuilder {
		private final Property property;

		public PropertyBuilder(Schema schema, Entity entity,
				PropertyType propertyType, String propertyName) {
			property = new Property(schema, entity, propertyType, propertyName);
		}

		public PropertyBuilder columnName(String columnName) {
			property.columnName = columnName;
			return this;
		}

		public PropertyBuilder columnType(String columnType) {
			property.columnType = columnType;
			return this;
		}

		public PropertyBuilder primaryKey() {
			property.primaryKey = true;
			return this;
		}

		public PropertyBuilder primaryKeyAsc() {
			property.primaryKey = true;
			property.pkAsc = true;
			return this;
		}

		public PropertyBuilder primaryKeyDesc() {
			property.primaryKey = true;
			property.pkDesc = true;
			return this;
		}

		public PropertyBuilder autoincrement() {
			if (!property.primaryKey
					|| property.propertyType != PropertyType.Long) {
				throw new RuntimeException(
						"AUTOINCREMENT is only available to primary key properties of type long/Long");
			}
			property.pkAutoincrement = true;
			return this;
		}

		public PropertyBuilder unique() {
			property.unique = true;
			return this;
		}

		public PropertyBuilder notNull() {
			property.notNull = true;
			return this;
		}

		public PropertyBuilder index() {
			Index index = new Index();
			index.addProperty(property);
			property.entity.addIndex(index);
			return this;
		}

		public PropertyBuilder indexAsc(String indexNameOrNull, boolean isUnique) {
			Index index = new Index();
			index.addPropertyAsc(property);
			if (isUnique) {
				index.makeUnique();
			}
			index.setName(indexNameOrNull);
			property.entity.addIndex(index);
			return this;
		}

		public PropertyBuilder indexDesc(String indexNameOrNull,
				boolean isUnique) {
			Index index = new Index();
			index.addPropertyDesc(property);
			if (isUnique) {
				index.makeUnique();
			}
			index.setName(indexNameOrNull);
			property.entity.addIndex(index);
			return this;
		}

		public Property getProperty() {
			return property;
		}

		public PropertyBuilder addValue(String name, int val) {
			if (property.propertyType != PropertyType.Enum) {
				throw new RuntimeException("only enum support this method!");
			}
			property.enumMap.put(name, val);
			return this;
		}
	}

	private final HashMap<String, Integer> enumMap = new HashMap<String, Integer>();
	private final Schema schema;
	private final Entity entity;
	private PropertyType propertyType;
	
	private String propertyName;
	private String columnName;
	private String columnType;

	private boolean primaryKey;
	private boolean pkAsc;
	private boolean pkDesc;
	private boolean pkAutoincrement;

	private boolean unique;
	private boolean notNull;

	/** Initialized in 2nd pass */
	private String constraints;

	private int ordinal;
	private String enumTypeName;
	private String className;
	private String javaType;

	public Property(Schema schema, Entity entity, PropertyType propertyType,
			String propertyName) {
		this.schema = schema;
		this.entity = entity;
		this.propertyName = uncapFirst(propertyName);
		this.propertyType = propertyType;
		this.className = entity.getClassName();
	}

	public static String capFirst(String str) {
		String cap = str.substring(0, 1).toUpperCase();
		return cap + str.substring(1);
	}

	public static String uncapFirst(String str) {
		String u = str.substring(0, 1).toLowerCase();
		return u + str.substring(1);
	}

	public String getPropertyName() {
		return propertyName;
	}

	public PropertyType getPropertyType() {
		return propertyType;
	}

	public void setPropertyType(PropertyType propertyType) {
		this.propertyType = propertyType;
	}

	void setPropertyName(String name) {
		this.propertyName = uncapFirst(name);
	}
	
	public String getColumnName() {
		return columnName;
	}

	public String getColumnType() {
		return columnType;
	}

	public boolean isPrimaryKey() {
		return primaryKey;
	}

	public boolean isAutoincrement() {
		return pkAutoincrement;
	}

	public String getConstraints() {
		return constraints;
	}

	public boolean isUnique() {
		return unique;
	}

	public boolean isNotNull() {
		return notNull;
	}

	public String getJavaType() {
		return javaType;
	}

	public String getEnumTypeName() {
		return enumTypeName;
	}
	
	public String getClassName() {
		return className;
	}

	public int getOrdinal() {
		return ordinal;
	}

	public HashMap<String, Integer> getEnumMap() {
		return enumMap;
	}

	public void setOrdinal(int ordinal) {
		this.ordinal = ordinal;
	}

	public Entity getEntity() {
		return entity;
	}

	void init2ndPass() {
		initConstraint();
		if (columnType == null) {
			columnType = schema.mapToDbType(propertyType);
		}
		if (columnName == null) {
			columnName = DaoUtil.dbName(propertyName);
		}
		
		if (propertyType == PropertyType.Enum) {
			enumTypeName = capFirst(propertyName);
			
			if (schema.getDefaultJavaPackage() != null
					&& !schema.getDefaultJavaPackage().trim().equals("")) {
				javaType = schema.getDefaultJavaPackage() + ".";
			}
			else { 
				javaType = ""; 
			}
			javaType = javaType + entity.getClassName() + "." + enumTypeName;
			return;
		}
		
		if (notNull) {
			javaType = schema.mapToJavaTypeNotNull(propertyType);
		} else {
			javaType = schema.mapToJavaTypeNullable(propertyType);
		}
	}

	private void initConstraint() {
		StringBuilder constraintBuilder = new StringBuilder();
		if (primaryKey) {
			constraintBuilder.append("PRIMARY KEY");
			if (pkAsc) {
				constraintBuilder.append(" ASC");
			}
			if (pkDesc) {
				constraintBuilder.append(" DESC");
			}
			if (pkAutoincrement) {
				constraintBuilder.append(" AUTOINCREMENT");
			}
		}
		// Always have String PKs NOT NULL because SQLite is pretty strange in
		// this respect:
		// One could insert multiple rows with NULL PKs
		if (notNull || (primaryKey && propertyType == PropertyType.String)) {
			constraintBuilder.append(" NOT NULL");
		}
		if (unique) {
			constraintBuilder.append(" UNIQUE");
		}
		String newContraints = constraintBuilder.toString().trim();
		if (constraintBuilder.length() > 0) {
			constraints = newContraints;
		}
	}

	void init3ndPass() {
		// Nothing to do so far
	}
	
	public Property clone() {
		Property prop = new Property(schema, entity, propertyType, propertyName);
		prop.init2ndPass();
		prop.init3ndPass();
		return prop;
	}

	@Override
	public String toString() {
		return "Property " + propertyName + " of " + entity.getClassName();
	}

}
