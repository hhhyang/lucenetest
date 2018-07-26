package com.lucenetest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LucenetestApplication {

	public static final Logger LOG = LoggerFactory.getLogger(LucenetestApplication.class);

	public static void main(String[] args) {

	    LOG.info("begin");
	    //SpringApplication.run(LucenetestApplication.class, args);
	    LOG.info("after");
	}
}
