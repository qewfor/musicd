/*
 * Copyright 2016 FuturemanGaming
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.futuremangaming.futurebot.data;

import java.sql.*;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;

public class DataBase implements AutoCloseable
{

    private Connection connection = null;
    private boolean available = true;
    private volatile String error = null;

    public DataBase(String ip, int port, String database, String username, String password)
    {
        connect(ip, port, database, username, password);
    }

    public void connect(String ip, int port, String database, String username, String password)
    {
        try
        {
            Class.forName("com.mysql.jdbc.Driver");
            connection = DriverManager.getConnection(String.format("jdbc:mysql://%s:%d/%s?user=%s&password=%s&useSSL=false",
                    ip, port, database, username, password));
            connection.setNetworkTimeout(Executors.newCachedThreadPool(), 5000);
        }
        catch (SQLException e)
        {
            error = e.getMessage();
            available = false;
        }
        catch (ClassNotFoundException e)
        {
            e.printStackTrace();
            available = false;
        }
    }

    public String getError()
    {
        return error;
    }

    public boolean isAvailable()
    {
        return available;
    }

    public boolean dropTable(String table)
    {
        return executeQuery("DROP TABLE IF EXISTS " + table + ";");
    }

    public Queue<String[]> readFromTable(String table, String... fields)
    {
        if (fields.length == 0 || table == null || table.isEmpty())
            return null;
        return retrieveQuery("SELECT " + String.join(", ", (CharSequence[]) fields) + " FROM " + table + ";");
    }

    public boolean insertInto(String tableExp, Object... values)
    {
        if (values.length == 0 || tableExp == null || tableExp.isEmpty())
            return false;
        CharSequence[] values0 = new CharSequence[values.length];
        for (int i = 0; i < values.length; i++)
        {
            if (values[i] instanceof String)
                values0[i] = "'" + sanitize((String) values[i]) + "'";
            else values0[i] = sanitize(values[i].toString());
        }
        return executeQuery("INSERT INTO " + tableExp + " VALUES(" + String.join(", ", values0) + ");");
    }

    public boolean removeFrom(String table, String where)
    {
        return !(table == null || where == null || table.isEmpty()) && executeQuery("DELETE FROM " + table + (where
                .isEmpty() ? "" : " WHERE " + where) + ";");
    }

    public boolean executeQuery(String query)
    {
        checkAvailable();
        try (Statement stmt = connection.createStatement())
        {
            return !stmt.execute(query);
        }
        catch (SQLTimeoutException e)
        {
            try
            {
                close();
            } catch (Exception ignored) {}
            available = false;
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
        return false;
    }

    public Queue<String[]> retrieveQuery(String query)
    {
        checkAvailable();
        try (Statement stmt = connection.createStatement())
        {
            ResultSet set = stmt.executeQuery(query);
            Queue<String[]> rows = new ConcurrentLinkedQueue<>();
            while (!set.isClosed() && set.next())
            {
                String[] columns = new String[set.getMetaData().getColumnCount()];
                for (int i = 0; i < columns.length; i++)
                    columns[i] = set.getString(i+1);
                rows.add(columns);
            }
            return rows;
        }
        catch (SQLTimeoutException e)
        {
            try
            {
                close();
            } catch (Exception ignored) {}
            available = false;
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
        return null;
    }

    private void checkAvailable()
    {
        if (!isAvailable())
            throw new IllegalStateException("Database connection not established.");
    }

    public static String sanitize(String s)
    {
        return s.replaceAll("(['\"`])", "\\$1");
    }

    @Override
    public void close() throws Exception
    {
        if (available)
            connection.close();
    }
}
