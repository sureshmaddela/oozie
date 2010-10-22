/**
 * Copyright (c) 2010 Yahoo! Inc. All rights reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License. See accompanying LICENSE file.
 */
package org.apache.oozie.action.hadoop;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RunningJob;

public class StreamingMain extends MapReduceMain {

    public static void main(String[] args) throws Exception {
        run(StreamingMain.class, args);
    }

    protected RunningJob submitJob(Configuration actionConf) throws Exception {
        JobConf jobConf = new JobConf();

        jobConf.set("mapred.mapper.class", "org.apache.hadoop.streaming.PipeMapper");
        jobConf.set("mapred.reducer.class", "org.apache.hadoop.streaming.PipeReducer");
        jobConf.set("mapred.map.runner.class", "org.apache.hadoop.streaming.PipeMapRunner");

        jobConf.set("mapred.input.format.class", "org.apache.hadoop.mapred.TextInputFormat");
        jobConf.set("mapred.output.format.class", "org.apache.hadoop.mapred.TextOutputFormat");
        jobConf.set("mapred.output.value.class", "org.apache.hadoop.io.Text");
        jobConf.set("mapred.output.key.class", "org.apache.hadoop.io.Text");

        jobConf.set("mapred.create.symlink", "yes");
        jobConf.set("mapred.used.genericoptionsparser", "true");

        jobConf.set("stream.addenvironment", "");

        String value = actionConf.get("oozie.streaming.mapper");
        if (value != null) {
            jobConf.set("stream.map.streamprocessor", value);
        }
        value = actionConf.get("oozie.streaming.reducer");
        if (value != null) {
            jobConf.set("stream.reduce.streamprocessor", value);
        }
        value = actionConf.get("oozie.streaming.record-reader");
        if (value != null) {
            jobConf.set("stream.recordreader.class", value);
        }
        String[] values = getStrings(actionConf, "oozie.streaming.record-reader-mapping");
        for (String s : values) {
            String[] kv = s.split("=");
            jobConf.set("stream.recordreader." + kv[0], kv[1]);
        }
        values = getStrings(actionConf, "oozie.streaming.env");
        value = jobConf.get("stream.addenvironment", "");
        if (value.length() > 0) {
            value = value + " ";
        }
        for (String s : values) {
            value = value + s + " ";
        }
        jobConf.set("stream.addenvironment", value);

        addActionConf(jobConf, actionConf);

        // propagate delegation related props from launcher job to MR job
        if (System.getenv("HADOOP_TOKEN_FILE_LOCATION") != null) {
            jobConf.set("mapreduce.job.credentials.binary", System.getenv("HADOOP_TOKEN_FILE_LOCATION"));
        }

        JobClient jobClient = null;
        RunningJob runJob = null;
        boolean exception = false;
        try {
            jobClient = createJobClient(jobConf);
            runJob = jobClient.submitJob(jobConf);
        }
        catch (Exception ex) {
            exception = true;
            throw ex;
        }
        finally {
            try {
                if (jobClient != null) {
                    jobClient.close();
                }
            }
            catch (Exception ex) {
                if (exception) {
                    System.out.println("JobClient Error: " + ex);
                }
                else {
                    throw ex;
                }
            }
        }
        return runJob;
    }

    public static void setStreaming(Configuration conf, String mapper, String reducer, String recordReader,
                                    String[] recordReaderMapping, String[] env) {
        if (mapper != null) {
            conf.set("oozie.streaming.mapper", mapper);
        }
        if (reducer != null) {
            conf.set("oozie.streaming.reducer", reducer);
        }
        if (recordReader != null) {
            conf.set("oozie.streaming.record-reader", recordReader);
        }
        setStrings(conf, "oozie.streaming.record-reader-mapping", recordReaderMapping);
        setStrings(conf, "oozie.streaming.env", env);
    }

}