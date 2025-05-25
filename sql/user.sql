CREATE TABLE IF NOT EXISTS users (
    id INT NOT NULL AUTO_INCREMENT,
    user_ID VARCHAR(20) NOT NULL,
    user_name VARCHAR(15) NOT NULL,
    password VARCHAR(40) NOT NULL,
    user_phone VARCHAR(15) NOT NULL,
    birth_date DATE NULL,
    profile_image LONGBLOB NULL, 
    PRIMARY KEY (id),
    UNIQUE INDEX username_UNIQUE (user_ID ASC) VISIBLE
);
