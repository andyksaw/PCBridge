package com.pcb.pcbridge.library.database;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.dbcp2.BasicDataSource;
import org.bukkit.Bukkit;

import com.pcb.pcbridge.PCBridge;
import com.pcb.pcbridge.library.AsyncAdapterParams;
import com.pcb.pcbridge.library.AsyncCallback;

/**
 * Adapter implementation for MySQL interactivity
 */

public class AdapterMySQL extends AbstractAdapter
{	
	private String _username;
	private String _password;
	private PCBridge _plugin;
	
	private BasicDataSource dataSource = new BasicDataSource();
		
	public AdapterMySQL(PCBridge plugin, String host, String port, String database, String username, String password)
	{
		this._username	= username;
		this._password 	= password;
		this._plugin 	= plugin;
		
		dataSource.setDriverClassName("com.mysql.jdbc.Driver");
		dataSource.setUrl("jdbc:mysql://" + host + ":" + port + "/" + database);
		dataSource.setUsername(_username);
		dataSource.setPassword(_password);
	}
	
	/**
	 * Gets a connection from the Connection Pool
	 * 
	 * @return
	 * @throws SQLException
	 */
	public Connection getConnection() throws SQLException
	{
		return dataSource.getConnection();
	}
	
	/**
	 * Maps the given arguments to an appropriate field-type, 
	 * then injects them into the provided prepared statement
	 * 
	 * @param statement
	 * @param args
	 * @return	 * 
	 * @throws SQLException
	 */
	private void MapParams(PreparedStatement statement, Object ... args) throws SQLException
	{
		if(args == null)
			return;
		
		int i = 1;
		for(Object arg : args)
		{
			if (arg instanceof Date) 
			{
				statement.setTimestamp(i++, new Timestamp( ((Date)arg).getTime() ));
		    } 
			else if (arg instanceof Integer) 
			{
				statement.setInt(i++, (Integer)arg);
		    } 
			else if (arg instanceof Long) 
			{
				statement.setLong(i++, (Long)arg);
		    } 
			else if (arg instanceof Double) 
			{
				statement.setDouble(i++, (Double)arg);
		    } 
			else if (arg instanceof Float) 
			{
				statement.setFloat(i++, (Float)arg);
		    }
			else 
			{
				statement.setString(i++, (String)arg);
		    }
		}
	}
	
	/**
	 * Executes a query on the current connection
	 * 
	 * @param sql	SQL query
	 * @param args	Query parameters to be injected
	 * @return	 * 
	 * @throws SQLException
	 */
	@Override
	public int Execute(String sql, Object... args) throws SQLException
	{
		try (
				Connection connection = getConnection();
				PreparedStatement statement = connection.prepareStatement(sql)
		) {
			MapParams(statement, args);
			
			return statement.executeUpdate();
		}
	}
	
	@Override
	public int Execute(String sql) throws SQLException
	{
		return Execute(sql, (Object)null);
	}
	
	/**
	 * Queries the current connection for a ResultSet
	 * 
	 * @param sql	SQL query
	 * @param args	Query parameters to be injected
	 * @return	 * 
	 * @throws SQLException
	 */
	@Override
	public List<HashMap<String, Object>> Query(String sql, Object... args) throws SQLException
	{
		try (
				Connection connection = getConnection();
				PreparedStatement statement = connection.prepareStatement(sql);
		) {
			MapParams(statement, args);
			
			try(ResultSet resultSet = statement.executeQuery())
			{
				return MapResultSet(resultSet);
			}
		}
	}
	
	@Override
	public List<HashMap<String, Object>> Query(String sql) throws SQLException
	{
		return Query(sql, (Object)null);
	}
	
	/**
	 * Queries the current connection for a row count
	 * 
	 * @param sql	SQL query
	 * @param args	Query parameters to be injected
	 * @return
	 * @throws SQLException
	 */
	@Override
	public int Count(String sql, Object... args) throws SQLException 
	{	
		int rowCount = 0;
		try (
				Connection connection = getConnection();
				PreparedStatement statement = connection.prepareStatement(sql);
		) {
			MapParams(statement, args);
			
			try(ResultSet resultSet = statement.executeQuery())
			{
				if(resultSet.next())
				{
					rowCount = resultSet.getInt(1);
				}
				else
				{
					rowCount = -1;
				}
			}
		}
		return rowCount;
	}

	@Override
	public int Count(String sql) throws SQLException 
	{
		return Count(sql, (Object)null);
	}
	
	
	/**
	 * Converts a ResultSet into a List<HashMap<String, Object>>
	 * (this allows continued use of data even after connection has been closed)
	 * 
	 * @param resultSet
	 * @return
	 * @throws SQLException
	 */
	private List<HashMap<String, Object>> MapResultSet(ResultSet resultSet) throws SQLException
	{
		ResultSetMetaData resultData = resultSet.getMetaData();
		int columnCount = resultData.getColumnCount();
		List<HashMap<String, Object>> rows = new ArrayList<HashMap<String, Object>>();
		
		while(resultSet.next())
		{
			HashMap<String, Object> row = new HashMap<String, Object>(columnCount);
			for(int x=1; x<=columnCount; x++)
			{
				row.put( resultData.getColumnName(x), resultSet.getObject(x) );
			}
			
			rows.add(row);
		}
		
		return rows;
	}
	
	
	/**
	 * Same as adapter Execute(), but asynchronous
	 * 
	 * @param callback
	 * @param sql
	 * @param args
	 */
	public void ExecuteAsync(final AsyncAdapterParams params, final AsyncCallback callback) 
	{
		Bukkit.getScheduler().runTaskAsynchronously(_plugin, new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					int result = Execute(params.SQL, params.Args);
					callback.OnSuccess(result);
				}
				catch(SQLException e)
				{
					callback.OnError(e);
				}
			}
		});
	}
	
	/**
	 * Same as adapter Query(), but asynchronous
	 */
	public void QueryAsync(final AsyncAdapterParams params, final AsyncCallback callback) 
	{
		Bukkit.getScheduler().runTaskAsynchronously(_plugin, new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					List<HashMap<String, Object>> results = Query(params.SQL, params.Args);
					callback.OnSuccess(results);
				}
				catch(SQLException e)
				{
					callback.OnError(e);
				}
			}
		});
	}
	
	/**
	 * Same as adapter Count(), but asynchronous
	 */
	public void CountAsync(final AsyncAdapterParams params, final AsyncCallback callback) 
	{
		Bukkit.getScheduler().runTaskAsynchronously(_plugin, new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					int result = Count(params.SQL, params.Args);
					callback.OnSuccess(result);
				}
				catch(SQLException e)
				{
					callback.OnError(e);
				}
			}
		});
	}
}
