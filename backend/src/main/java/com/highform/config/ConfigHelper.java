// Copyright at HighForm Inc 2013. All rights reserved.

package com.highform.config;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.highform.proto.ProtoLoader;
import com.google.common.base.Preconditions;
import com.google.protobuf.Message;

/**
 *
 * @author Fei
 */
public class ConfigHelper {
  private final static Logger log = LoggerFactory.getLogger(ConfigHelper.class);

  /**
   * This helper will handle exceptions when loading configs from S3/Disk
   * 
   * @param configFile
   * @param builder
   * @return
   */
  public static <T extends Message.Builder> Message loadConfig(String configFile, T builder) {
    Preconditions.checkArgument(StringUtils.isNotBlank(configFile));
    Preconditions.checkNotNull(builder);
    try {
      Message config = ProtoLoader.load(configFile, builder).build();
      return config;
    } catch (FileNotFoundException e) {
      log.error("Failed to find the config file", e);
      return null;
    } catch (IOException e) {
      log.error("Failed to load the config file", e);
      return null;
    }
  }

}