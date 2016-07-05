/* 
 * Copyright 2016 King's College London, Richard Jackson <richgjackson@gmail.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.kcl.it;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import uk.ac.kcl.model.Document;
import uk.ac.kcl.mutators.*;
import uk.ac.kcl.rowmappers.DocumentRowMapper;
import uk.ac.kcl.scheduling.SingleJobLauncher;
import uk.ac.kcl.service.ElasticGazetteerService;
import uk.ac.kcl.testexecutionlisteners.PostgresDeidTestExecutionListener;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.util.*;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 *
 * @author rich
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ComponentScan("uk.ac.kcl.it")
@TestPropertySource({
        "classpath:deidPKprofiles.properties",
        "classpath:postgres_test_config_deid.properties",
        "classpath:jms.properties",
        "classpath:noScheduling.properties",
        "classpath:gate.properties",
        "classpath:deidentification.properties",
        "classpath:postgres_db.properties",
        "classpath:elasticsearch.properties",
        "classpath:jobAndStep.properties"})
@ContextConfiguration(classes = {
        SingleJobLauncher.class,
        SqlServerTestUtils.class,
        TestUtils.class},
        loader = AnnotationConfigContextLoader.class)
public class PostgresIntegrationTestsElasticGazetteerPerformanceTest {


    @Autowired
    @Qualifier("sourceDataSource")
    public DataSource sourceDataSource;

    @Autowired
    DocumentRowMapper documentRowMapper;

    @Autowired
    ElasticGazetteerService elasticGazetteerService;

    @Autowired
    TestUtils testUtils;

    @Autowired
    PostGresTestUtils postGresTestUtils;




    @Test
    @DirtiesContext
    public void postgresIntegrationTestsDeIdentificationPKPartitionWithoutScheduling() {
        postGresTestUtils.createBasicInputTable();
        postGresTestUtils.createBasicOutputTable();
        postGresTestUtils.createDeIdInputTable();
        List<Mutant> mutants  = testUtils.insertTestDataForDeidentification("tblIdentifiers","tblInputDocs");
        for(Mutant mutant : mutants){
            Set<Pattern> patterns = new HashSet<>();
            mutant.setDeidentifiedString(elasticGazetteerService.deIdentifyString(mutant.getFinalText(),String.valueOf(mutant.getDocumentid())));
            Set<String> set = new HashSet<>(mutant.getOutputTokens());
            patterns.addAll(set.stream().map(string -> Pattern.compile(Pattern.quote(string), Pattern.CASE_INSENSITIVE)).collect(Collectors.toSet()));
            List<MatchResult> results = new ArrayList<>();
            for (Pattern pattern : patterns) {
                Matcher matcher = pattern.matcher(mutant.getDeidentifiedString());
                while (matcher.find()){
                    results.add(matcher.toMatchResult());
                }
            }
            System.out.println("Doc ID "+ mutant.getDocumentid() + " has " +results.size() +" unmasked strings from a total of " + mutant.getOutputTokens().size());
            System.out.println(mutant.getDeidentifiedString());
            System.out.println(mutant.getFinalText());
            System.out.println(mutant.getInputTokens());
            System.out.println(mutant.getOutputTokens());
        }

    }



}