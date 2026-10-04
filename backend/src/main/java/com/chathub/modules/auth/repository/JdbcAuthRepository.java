package com.chathub.modules.auth.repository;

import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** JdbcTemplate implementation of {@link AuthRepository}. */
@Repository
public class JdbcAuthRepository implements AuthRepository {

	private static final String USER_COLUMNS = "id, username, email, password_hash";

	private static final RowMapper<UserRecord> USER_ROW_MAPPER = (rs, rowNum) -> new UserRecord(rs.getInt("id"),
			rs.getString("username"), rs.getString("email"), rs.getString("password_hash"));

	private final JdbcTemplate jdbc;

	public JdbcAuthRepository(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public Optional<UserRecord> findByEmail(String email) {
		return jdbc.query("select " + USER_COLUMNS + " from users where email = ?", USER_ROW_MAPPER, email).stream()
				.findFirst();
	}

	@Override
	public Optional<UserRecord> findByUsername(String username) {
		return jdbc.query("select " + USER_COLUMNS + " from users where username = ?", USER_ROW_MAPPER, username)
				.stream().findFirst();
	}

	@Override
	public UserRecord createUser(String username, String email, String passwordHash) {
		return jdbc.queryForObject(
				"insert into users (username, email, password_hash) values (?, ?, ?) returning " + USER_COLUMNS,
				USER_ROW_MAPPER, username, email, passwordHash);
	}
}
