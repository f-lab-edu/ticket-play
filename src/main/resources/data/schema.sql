CREATE TABLE IF NOT EXISTS concert  (
                                        id INT AUTO_INCREMENT PRIMARY KEY,
                                        name VARCHAR(200) NOT NULL,
    artist_name VARCHAR(20) NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    reservation_start_time DATETIME NOT NULL,
    reservation_end_time DATETIME NOT NULL,
    remaining_seat TINYINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    concert_hall_name VARCHAR(30) NOT NULL,
    concert_hall_address VARCHAR(200) NOT NULL,
    concert_seat_info TEXT NOT NULL,
    version BIGINT DEFAULT 0 NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT NULL,
    deleted_at TIMESTAMP NULL DEFAULT NULL
    );

CREATE TABLE IF NOT EXISTS concert_seat (
                                            id INT AUTO_INCREMENT PRIMARY KEY,
                                            concert_id INT,
                                            grade VARCHAR(20),
    seat_number TINYINT,
    status VARCHAR(20),
    version BIGINT DEFAULT 0 NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT NULL,
    deleted_at TIMESTAMP NULL
    );

CREATE TABLE IF NOT EXISTS member (
                                      id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                      email VARCHAR(120) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    version BIGINT DEFAULT 0 NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT NULL,
    deleted_at TIMESTAMP DEFAULT NULL
    );

CREATE TABLE IF NOT EXISTS ticket_reservation (
                                                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                  member_id BIGINT NOT NULL,
                                                  seat_id BIGINT NOT NULL,
                                                  status VARCHAR(20) NOT NULL,
    version BIGINT DEFAULT 0 NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT NULL,
    deleted_at TIMESTAMP NULL DEFAULT NULL
    );

CREATE TABLE IF NOT EXISTS ticket_waiting (
                                              id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                              concert_id BIGINT NOT NULL,
                                              member_id BIGINT NOT NULL,
                                              waiting_number INT NOT NULL,
                                              status VARCHAR(20) NOT NULL,
    version BIGINT DEFAULT 0 NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT NULL,
    deleted_at TIMESTAMP NULL DEFAULT NULL
    );