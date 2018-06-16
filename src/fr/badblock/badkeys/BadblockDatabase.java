package fr.badblock.badkeys;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import fr.badblock.badkeys.Request.RequestType;


public class BadblockDatabase {

	private static BadblockDatabase instance;

	// Instance
	private Connection connection;
	private boolean isConnected;

	List<Thread> threads;
	// Queue<Request> requests;
	Thread thread = null;

	public BadblockDatabase() {
		threads = new ArrayList<>();
		/*
		 * requests = Queues.newLinkedBlockingDeque(); for (int i = 0; i < 16;
		 * i++) { thread = new Thread("database_" + new
		 * Random().nextInt(99999999)) {
		 * 
		 * @Override public void run() { synchronized (thread) { while (true) {
		 * boolean isEmpty = true; synchronized (requests) { isEmpty =
		 * requests.isEmpty(); } if (!isEmpty) { Request request = null;
		 * synchronized (requests) { request = requests.poll(); } if (request !=
		 * null) { try { Statement statement = createStatement(); if
		 * (request.getRequestType().equals(RequestType.SETTER)) {
		 * statement.executeUpdate(request.getRequest()); }else{ ResultSet
		 * resultSet = statement.executeQuery(request.getRequest());
		 * request.done(resultSet); resultSet.close(); } statement.close();
		 * }catch(Exception error) { error.printStackTrace(); } } } try {
		 * thread.wait(); } catch (InterruptedException e) {
		 * e.printStackTrace(); } } } } }; threads.add(thread); thread.start();
		 * }
		 */
	}

	public String mysql_real_escape_string(String str) {
		if (str == null) {
			return null;
		}

		if (str.replaceAll("[a-zA-Z0-9_!@#$%^&*()-=+~.;:,\\Q[\\E\\Q]\\E<>{}\\/? ]", "").length() < 1) {
			return str;
		}

		String clean_string = str;
		clean_string = clean_string.replaceAll("\\\\", "\\\\\\\\");
		clean_string = clean_string.replaceAll("\\n", "\\\\n");
		clean_string = clean_string.replaceAll("\\r", "\\\\r");
		clean_string = clean_string.replaceAll("\\t", "\\\\t");
		clean_string = clean_string.replaceAll("\\00", "\\\\0");
		clean_string = clean_string.replaceAll("'", "\\\\'");
		clean_string = clean_string.replaceAll("\\\"", "\\\\\"");

		if (clean_string.replaceAll("[a-zA-Z0-9_!@#$%^&*()-=+~.;:,\\Q[\\E\\Q]\\E<>{}\\/?\\\\\"' ]", "").length() < 1) {
			return clean_string;
		}
		try {
			java.sql.Statement stmt = connection.createStatement();
			String qry = "SELECT QUOTE('" + clean_string + "')";

			stmt.executeQuery(qry);
			java.sql.ResultSet resultSet = stmt.getResultSet();
			resultSet.first();
			String r = resultSet.getString(1);
			return r.substring(1, r.length() - 1);
		} catch (Exception error) {
			error.printStackTrace();
			return str;
		}
	}

	public void addRequest(Request request) {
		new Thread() {
			@Override
			public void run() {
				addSyncRequest(request);
			}
		}.start();
	}

	public void addSyncRequest(Request request) {
		try {
			Statement statement = createStatement();
			if (request.getRequestType().equals(RequestType.SETTER)) {
				statement.executeUpdate(request.getRequest());
			} else {
				ResultSet resultSet = statement.executeQuery(request.getRequest());
				request.done(resultSet);
				if (!request.isDoNotClosed())
					resultSet.close();
			}
			statement.close();
		} catch (Exception error) {
			error.printStackTrace();
		}
	}

	public static BadblockDatabase getInstance() {
		if (instance == null)
			instance = new BadblockDatabase();
		return instance;
	}

	/**
	 * Connexion � la base de donn�es
	 * 
	 * @param hostName
	 * @param port
	 * @param username
	 * @param password
	 * @param database
	 */
	public void connect(String hostName, int port, String username, String password, String database) {
		if (port == 0)
			return; // retourne si le port est vide.
		try {
			Class.forName("com.mysql.jdbc.Driver");
			DriverManager.setLoginTimeout(10);
			connection = DriverManager.getConnection("jdbc:mysql://" + hostName + ":" + port + "/"
					+ database + "?autoReconnect=true&connectTimeout=5000", username, password);
			isConnected = true;
			System.out.print("a");
		} catch (Exception e) {
			e.printStackTrace();
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		new Thread() {
			@Override
			public void run() {
				while (true) {
					try {
						if (isConnectionEtablished()) {
							try {
								Thread.sleep(1000);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							continue;
						}
					} catch (SQLException e1) {
						e1.printStackTrace();
					}
					try {
						Class.forName("com.mysql.jdbc.Driver");
						DriverManager.setLoginTimeout(10);
						connection = DriverManager.getConnection("jdbc:mysql://" + hostName + ":" + port + "/"
								+ database + "?autoReconnect=true&connectTimeout=5000", username, password);
						isConnected = true;
					} catch (Exception e) {
						e.printStackTrace();
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e1) {
							e1.printStackTrace();
						}
					}
				}
			}
		}.start();
	}

	/**
	 * V�rifier si la personne est connect�e � la base de donn�e. Attention:
	 * m�thode d�capr�c�e car elle ne permet pas de savoir si la personne est
	 * r�ellement connect�e encore.
	 * 
	 * @deprecated
	 * @return
	 */
	@Deprecated
	public boolean isConnected() {
		return this.isConnected;
	}

	/**
	 * V�rifier si la connexion est active.
	 * 
	 * @return
	 * @throws SQLException
	 */
	public boolean isConnectionEtablished() throws SQLException {
		return this.connection != null && !this.connection.isClosed();
	}

	/**
	 * Cr�er un statement reli�e � la connexion de la base de donn�e
	 * 
	 * @return
	 * @throws SQLException
	 */
	public Statement createStatement() throws SQLException {
		if (!isConnected || (connection != null && connection.isClosed())) {
			throw new DatabaseIsNotConnectedException();
		}
		Statement statement = connection.createStatement();
		return statement;
	}

	/**
	 * Terminer la connexion, cela terminera la liaison avec la base de donn�es
	 * ainsi que les statements et requ�tes en cours sur la session.
	 * 
	 * @throws SQLException
	 */
	public void closeConnection() throws SQLException {
		connection.close();
	}

	/**
	 * R�cup�rer la connexion active
	 * 
	 * @return
	 */
	public Connection getConnection() {
		return this.connection;
	}

	/**
	 * 
	 * @author Aurelian User
	 *
	 */
	@SuppressWarnings("serial")
	private class DatabaseIsNotConnectedException extends RuntimeException {
		public DatabaseIsNotConnectedException() {
			super("Not connected");
		}
	}

}
