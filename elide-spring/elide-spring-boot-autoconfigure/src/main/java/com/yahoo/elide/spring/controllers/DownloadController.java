/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.spring.controllers;

import com.yahoo.elide.async.service.ResultStorageEngine;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;

@Slf4j
@Configuration
@RestController
@RequestMapping(value = "${elide.async.download.path:download}")
@ConditionalOnExpression("${elide.async.download.enabled:false}")
public class DownloadController {

    private ResultStorageEngine resultStorageEngine;

    @Autowired
    public DownloadController(ResultStorageEngine resultStorageEngine) {
        log.debug("Started ~~");
        this.resultStorageEngine = resultStorageEngine;
    }

    @GetMapping(path = "/{asyncQueryId}")
    public void download(@PathVariable String asyncQueryId, HttpServletResponse response)
            throws SQLException, IOException {

        ///************* Getresults from ResultStorageEngine
        byte[] temp = resultStorageEngine.getResultsByID(asyncQueryId);
        PrintWriter writer = response.getWriter();
        String reconstructedStr = "";
        if (temp == null) {
            response.setContentType(MediaType.APPLICATION_JSON);
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Result not found");
        } else {
            reconstructedStr = new String(temp);
            response.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            response.setHeader("Content-Disposition", "attachment; filename=" + asyncQueryId);
        }
        writer.write(reconstructedStr);
    }
}
