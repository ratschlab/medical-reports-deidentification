CREATE TABLE IF NOT EXISTS sample_reports (
    `reportnr` character varying(40) NOT NULL,
    `additional_col` character varying(100),
    `dat` timestamp without time zone,
    `fcode` character varying(10),
    `content` character varying(1000)
);
CREATE TABLE IF NOT EXISTS processed_reports (
    `reportnr` character varying(40) NOT NULL,
    `additional_col` character varying(100),
    `dat` timestamp without time zone,
    `fcode` character varying(10),
    `content` character varying(1000)
);

