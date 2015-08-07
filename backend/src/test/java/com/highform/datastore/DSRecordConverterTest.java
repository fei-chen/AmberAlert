/**
 * Copyright at HighForm Inc. All rights reserved Jun 22, 2013.
 */

package com.highform.datastore;

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;

import com.highform.datastore.proto.Datastore.DataType;
import com.highform.datastore.proto.Datastore.Field;
import com.highform.datastore.proto.Datastore.FieldSchema;
import com.highform.datastore.proto.Datastore.PrimaryKey;
import com.highform.datastore.proto.Datastore.TableSchema;
import com.highform.university.proto.CourseInfo.Course;
import com.highform.university.proto.CourseInfo.Department;
import com.highform.university.proto.CourseInfo.Section;
import com.highform.university.proto.CourseInfo.Term;
import com.highform.university.proto.CourseInfo.University;

/**
 * DSRecordConverter unit tests
 *
 * @author fei
 */
public class DSRecordConverterTest {
  private static TableSchemaMap schemaMap;

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    TableSchema schema = TableSchema.newBuilder().setName("course_info")
        .setPrimaryKey(PrimaryKey.newBuilder().addFieldSchemaIndex(0))
        .addFieldSchema(FieldSchema.newBuilder()
            .setName("course_name")
            .setType(DataType.TEXT))
        .addFieldSchema(FieldSchema.newBuilder().setName("section_id").setType(DataType.INT))
        .build();
    schemaMap = new TableSchemaMap(schema);
  }

  @Test
  public void testGetProtoMap() {
    Section section = Section.newBuilder()
        .setKey("section_key")
        .setId(Field.newBuilder()
            .setName("section_id")
            .setValue("100"))
        .setCourse(Course.newBuilder()
            .setKey("course_key")
            .setName(Field.newBuilder()
                .setName("course_name")
                .setValue("math")))
        .setDepartment(Department.newBuilder()
            .setKey("department_key"))
        .setTerm(Term.newBuilder()
            .setKey("term_key"))
        .setUniversity(University.newBuilder()
            .setKey("university_key"))
        .build();

    DSRecord<Object> record = DSRecordConverter.getDSRecord(section, schemaMap);
    assertEquals(100, record.getFieldValue("section_id"));
    assertEquals("math", record.getFieldValue("course_name"));
  }

  @Test
  public void testGetProtoKey() {
    Section section = Section.newBuilder()
        .setKey("section_key")
        .setId(Field.newBuilder()
            .setName("section_id")
            .setValue("100"))
        .setCourse(Course.newBuilder()
            .setKey("course_key")
            .setName(Field.newBuilder()
                .setName("course_name")
                .setValue("math")))
        .setUniversity(University.newBuilder()
            .setKey("university_key"))
        .setDepartment(Department.newBuilder()
            .setKey("department_key"))
        .setTerm(Term.newBuilder()
            .setKey("term_key"))
        .build();

    DSRecord<Object> record = DSRecordConverter.getDSRecord(section, schemaMap);
    assertEquals("section_key,course_key,department_key,term_key,university_key", record.getKey());
  }
}
