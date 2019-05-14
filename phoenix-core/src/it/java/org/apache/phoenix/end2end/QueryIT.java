/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.phoenix.end2end;

import static org.apache.phoenix.util.TestUtil.B_VALUE;
import static org.apache.phoenix.util.TestUtil.C_VALUE;
import static org.apache.phoenix.util.TestUtil.E_VALUE;
import static org.apache.phoenix.util.TestUtil.ROW1;
import static org.apache.phoenix.util.TestUtil.ROW2;
import static org.apache.phoenix.util.TestUtil.ROW3;
import static org.apache.phoenix.util.TestUtil.ROW4;
import static org.apache.phoenix.util.TestUtil.ROW5;
import static org.apache.phoenix.util.TestUtil.ROW6;
import static org.apache.phoenix.util.TestUtil.ROW7;
import static org.apache.phoenix.util.TestUtil.ROW8;
import static org.apache.phoenix.util.TestUtil.ROW9;
import static org.apache.phoenix.util.TestUtil.TEST_PROPERTIES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Properties;

import org.apache.phoenix.exception.SQLExceptionCode;
import org.apache.phoenix.util.PropertiesUtil;
import org.junit.Test;


/**
 * 
 * Basic tests for Phoenix JDBC implementation
 *
 */
public class QueryIT extends BaseQueryIT {
    
    public QueryIT(String indexDDL, boolean mutable, boolean columnEncoded) throws Exception {
        super(indexDDL, mutable, columnEncoded, false);
    }
    
    @Test
    public void testIntFilter() throws Exception {
        String updateStmt = 
            "upsert into " + tableName +
            " (" +
            "    ORGANIZATION_ID, " +
            "    ENTITY_ID, " +
            "    A_INTEGER) " +
            "VALUES (?, ?, ?)";
        // Override value that was set at creation time
        Properties props = PropertiesUtil.deepCopy(TEST_PROPERTIES);
        Connection conn = DriverManager.getConnection(url, props);
        PreparedStatement stmt = conn.prepareStatement(updateStmt);
        stmt.setString(1, tenantId);
        stmt.setString(2, ROW4);
        stmt.setInt(3, -10);
        stmt.execute();
        conn.commit();
        
        String query = "SELECT entity_id FROM " + tableName + " WHERE organization_id=? and a_integer >= ?";
        PreparedStatement statement = conn.prepareStatement(query);
        statement.setString(1, tenantId);
        statement.setInt(2, 7);
        ResultSet rs = statement.executeQuery();
        assertValueEqualsResultSet(rs, Arrays.<Object>asList(ROW7, ROW8, ROW9));

        query = "SELECT entity_id FROM " + tableName + " WHERE organization_id=? and a_integer < 2";
        statement = conn.prepareStatement(query);
        statement.setString(1, tenantId);
        rs = statement.executeQuery();
        assertValueEqualsResultSet(rs, Arrays.<Object>asList(ROW1, ROW4));

        query = "SELECT entity_id FROM " + tableName + " WHERE organization_id=? and a_integer <= 2";
        statement = conn.prepareStatement(query);
        statement.setString(1, tenantId);
        rs = statement.executeQuery();
        assertValueEqualsResultSet(rs, Arrays.<Object>asList(ROW1, ROW2, ROW4));

        query = "SELECT entity_id FROM " + tableName + " WHERE organization_id=? and a_integer >=9";
        statement = conn.prepareStatement(query);
        statement.setString(1, tenantId);
        rs = statement.executeQuery();
        assertTrue (rs.next());
        assertEquals(rs.getString(1), ROW9);
        assertFalse(rs.next());
        conn.close();
    }
    
    @Test
    public void testToDateOnString() throws Exception { // TODO: test more conversion combinations
        String query = "SELECT a_string FROM " + tableName + " WHERE organization_id=? and a_integer = 5";
        Properties props = PropertiesUtil.deepCopy(TEST_PROPERTIES);
        Connection conn = DriverManager.getConnection(getUrl(), props);
        try {
            PreparedStatement statement = conn.prepareStatement(query);
            statement.setString(1, tenantId);
            ResultSet rs = statement.executeQuery();
            assertTrue (rs.next());
            rs.getDate(1);
            fail();
        } catch (SQLException e) { // Expected
            assertEquals(SQLExceptionCode.TYPE_MISMATCH.getErrorCode(),e.getErrorCode());
        } finally {
            conn.close();
        }
    }

    @Test
    public void testColumnOnBothSides() throws Exception {
        String query = "SELECT entity_id FROM " + tableName + " WHERE organization_id=? and a_string = b_string";
        Properties props = PropertiesUtil.deepCopy(TEST_PROPERTIES);
        Connection conn = DriverManager.getConnection(getUrl(), props);
        try {
            PreparedStatement statement = conn.prepareStatement(query);
            statement.setString(1, tenantId);
            ResultSet rs = statement.executeQuery();
            assertTrue (rs.next());
            assertEquals(rs.getString(1), ROW7);
            assertFalse(rs.next());
        } finally {
            conn.close();
        }
    }

    @Test
    public void testDateInList() throws Exception {
        String query = "SELECT entity_id FROM " + tableName + " WHERE a_date IN (?,?) AND a_integer < 4";
        Properties props = PropertiesUtil.deepCopy(TEST_PROPERTIES);
        Connection conn = DriverManager.getConnection(url, props);
        try {
            PreparedStatement statement = conn.prepareStatement(query);
            statement.setDate(1, new Date(0));
            statement.setDate(2, date);
            ResultSet rs = statement.executeQuery();
            assertTrue(rs.next());
            assertEquals(ROW1, rs.getString(1));
            assertFalse(rs.next());
        } finally {
            conn.close();
        }
    }
    
    @Test
    public void testSimpleInListStatement() throws Exception {
        String query = "SELECT entity_id FROM " + tableName + " WHERE organization_id=? AND a_integer IN (2,4)";
        Properties props = PropertiesUtil.deepCopy(TEST_PROPERTIES);
        Connection conn = DriverManager.getConnection(url, props);
        try {
            PreparedStatement statement = conn.prepareStatement(query);
            statement.setString(1, tenantId);
            ResultSet rs = statement.executeQuery();
            assertValueEqualsResultSet(rs, Arrays.<Object>asList(ROW2, ROW4));
        } finally {
            conn.close();
        }
    }
    
    @Test
    public void testPartiallyQualifiedRVCInList() throws Exception {
        String query = "SELECT entity_id FROM " + tableName + " WHERE (a_integer,a_string) IN ((2,'a'),(5,'b'))";
        Properties props = PropertiesUtil.deepCopy(TEST_PROPERTIES);
        Connection conn = DriverManager.getConnection(url, props);
        try {
            PreparedStatement statement = conn.prepareStatement(query);
            ResultSet rs = statement.executeQuery();
            assertValueEqualsResultSet(rs, Arrays.<Object>asList(ROW2, ROW5));
        } finally {
            conn.close();
        }
    }
    
    @Test
    public void testFullyQualifiedRVCInList() throws Exception {
        String query = "SELECT entity_id FROM " + tableName + " WHERE (a_integer,a_string, organization_id,entity_id) IN ((2,'a',:1,:2),(5,'b',:1,:3))";
        Properties props = PropertiesUtil.deepCopy(TEST_PROPERTIES);
        Connection conn = DriverManager.getConnection(url, props);
        try {
            PreparedStatement statement = conn.prepareStatement(query);
            statement.setString(1, tenantId);
            statement.setString(2, ROW2);
            statement.setString(3, ROW5);
            ResultSet rs = statement.executeQuery();
            assertValueEqualsResultSet(rs, Arrays.<Object>asList(ROW2, ROW5));
        } finally {
            conn.close();
        }
    }
    
    @Test
    public void testOneInListStatement() throws Exception {
        String query = "SELECT entity_id FROM " + tableName + " WHERE organization_id=? AND b_string IN (?)";
        Properties props = PropertiesUtil.deepCopy(TEST_PROPERTIES);
        Connection conn = DriverManager.getConnection(url, props);
        try {
            PreparedStatement statement = conn.prepareStatement(query);
            statement.setString(1, tenantId);
            statement.setString(2, E_VALUE);
            ResultSet rs = statement.executeQuery();
            assertTrue(rs.next());
            assertEquals(ROW3, rs.getString(1));
            assertTrue(rs.next());
            assertEquals(ROW6, rs.getString(1));
            assertTrue(rs.next());
            assertEquals(ROW9, rs.getString(1));
            assertFalse(rs.next());
        } finally {
            conn.close();
        }
    }

    
    @Test
    public void testMixedTypeInListStatement() throws Exception {
        String query = "SELECT entity_id FROM " + tableName + " WHERE organization_id=? AND x_long IN (5, ?)";
        Properties props = PropertiesUtil.deepCopy(TEST_PROPERTIES);
        Connection conn = DriverManager.getConnection(url, props);
        try {
            PreparedStatement statement = conn.prepareStatement(query);
            statement.setString(1, tenantId);
            long l = Integer.MAX_VALUE + 1L;
            statement.setLong(2, l);
            ResultSet rs = statement.executeQuery();
            assertTrue(rs.next());
            assertEquals(ROW7, rs.getString(1));
            assertTrue(rs.next());
            assertEquals(ROW9, rs.getString(1));
            assertFalse(rs.next());
        } finally {
            conn.close();
        }
    }
    
    @Test
    public void testValidStringConcatExpression() throws Exception {//test fails with stack overflow wee
        int counter=0;
        String[] answers = new String[]{"00D300000000XHP5bar","a5bar","15bar","5bar","5bar"};
        String[] queries = new String[] { 
        		"SELECT  organization_id || 5 || 'bar' FROM " + tableName + " limit 1",
        		"SELECT a_string || 5 || 'bar' FROM " + tableName + "  order by a_string  limit 1",
        		"SELECT a_integer||5||'bar' FROM " + tableName + " order by a_integer  limit 1",
        		"SELECT x_decimal||5||'bar' FROM " + tableName + " limit 1",
        		"SELECT x_long||5||'bar' FROM " + tableName + " limit 1"
        };

        for (String query : queries) {
        	Properties props = new Properties();
        	Connection conn = DriverManager.getConnection(getUrl(), props);
        	try {
        		PreparedStatement statement = conn.prepareStatement(query);
        		ResultSet rs=statement.executeQuery();
        		assertTrue(rs.next());
        		assertEquals(answers[counter++],rs.getString(1));
           		assertFalse(rs.next());
        	}
        	finally {
        		conn.close();
        	}
        }
    }
    
    @Test
    public void testRowKeySingleIn() throws Exception {
        String query = "SELECT entity_id FROM " + tableName + " WHERE organization_id=? and entity_id IN (?,?,?)";
        Properties props = PropertiesUtil.deepCopy(TEST_PROPERTIES);
        Connection conn = DriverManager.getConnection(getUrl(), props);
        try {
            PreparedStatement statement = conn.prepareStatement(query);
            statement.setString(1, tenantId);
            statement.setString(2, ROW2);
            statement.setString(3, ROW6);
            statement.setString(4, ROW8);
            ResultSet rs = statement.executeQuery();
            assertTrue (rs.next());
            assertEquals(rs.getString(1), ROW2);
            assertTrue (rs.next());
            assertEquals(rs.getString(1), ROW6);
            assertTrue (rs.next());
            assertEquals(rs.getString(1), ROW8);
            assertFalse(rs.next());
        } finally {
            conn.close();
        }
    }
    
    
    @Test
    public void testRowKeyMultiIn() throws Exception {
        String query = "SELECT entity_id FROM " + tableName + " WHERE organization_id=? and entity_id IN (?,?,?) and a_string IN (?,?)";
        Properties props = PropertiesUtil.deepCopy(TEST_PROPERTIES);
        Connection conn = DriverManager.getConnection(getUrl(), props);
        try {
            PreparedStatement statement = conn.prepareStatement(query);
            statement.setString(1, tenantId);
            statement.setString(2, ROW2);
            statement.setString(3, ROW6);
            statement.setString(4, ROW9);
            statement.setString(5, B_VALUE);
            statement.setString(6, C_VALUE);
            ResultSet rs = statement.executeQuery();
            assertTrue (rs.next());
            assertEquals(rs.getString(1), ROW6);
            assertTrue (rs.next());
            assertEquals(rs.getString(1), ROW9);
            assertFalse(rs.next());
        } finally {
            conn.close();
        }
    }
    
    @Test
    public void testColumnAliasMapping() throws Exception {
        String query = "SELECT a.a_string, " + tableName + ".b_string FROM " + tableName + " a WHERE ?=organization_id and 5=a_integer ORDER BY a_string, b_string";
        Properties props = PropertiesUtil.deepCopy(TEST_PROPERTIES);
        Connection conn = DriverManager.getConnection(getUrl(), props);
        try {
            PreparedStatement statement = conn.prepareStatement(query);
            statement.setString(1, tenantId);
            ResultSet rs = statement.executeQuery();
            assertTrue (rs.next());
            assertEquals(rs.getString(1), B_VALUE);
            assertEquals(rs.getString("B_string"), C_VALUE);
            assertFalse(rs.next());
        } finally {
            conn.close();
        }
    }
}
