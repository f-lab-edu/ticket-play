CREATE TABLE concert (
                         id INT AUTO_INCREMENT PRIMARY KEY,
                         name VARCHAR(200) NOT NULL,
                         artist_name VARCHAR(20) NOT NULL,
                         start_time DATETIME NOT NULL,
                         end_time DATETIME NOT NULL,
                         reservation_start_time DATETIME NOT NULL,
                         reservation_end_time DATETIME NOT NULL,
                         remaining_seat TINYINT(6) NOT NULL,
                         status VARCHAR(20) NOT NULL,
                         concert_hall_name VARCHAR(30) NOT NULL,
                         concert_hall_address VARCHAR(200) NOT NULL,
                         concert_seat_info TEXT NOT NULL,
                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMP DEFAULT DEFAULT NULL,
                         deleted_at TIMESTAMP NULL DEFAULT NULL
);

CREATE TABLE concert_seat (
                              id INT AUTO_INCREMENT PRIMARY KEY,
                              concert_id INT,
                              grade VARCHAR(20),
                              seat_number TINYINT(4),
                              created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                              updated_at TIMESTAMP DEFAULT DEFAULT NULL,
                              deleted_at TIMESTAMP NULL
);

-- 테이블 생성
CREATE TABLE member (
                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                       email VARCHAR(120) NOT NULL UNIQUE,
                       password VARCHAR(255) NOT NULL,
                       name VARCHAR(100) NOT NULL,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP DEFAULT DEFAULT NULL,
                       deleted_at TIMESTAMP NULL
);

CREATE TABLE ticket_reservation (
                                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                    user_id BIGINT NOT NULL,
                                    ticket_id BIGINT NOT NULL,
                                    status VARCHAR(20) NOT NULL,
                                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                    updated_at TIMESTAMP DEFAULT DEFAULT NULL,
                                    deleted_at TIMESTAMP NULL DEFAULT NULL,
);

CREATE TABLE ticket_waiting (
                              id BIGINT PRIMARY KEY AUTO_INCREMENT,
                              concert_id BIGINT NOT NULL,
                              user_id BIGINT NOT NULL,
                              waiting_number INT NOT NULL,
                              status VARCHAR(20) NOT NULL,
                              created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                              updated_at TIMESTAMP NULL DEFAULT NULL,
                              deleted_at TIMESTAMP NULL DEFAULT NULL,
);