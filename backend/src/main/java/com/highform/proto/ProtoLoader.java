// Copyright at HighForm Inc. All rights reserved 2013.

package com.highform.proto;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.apache.commons.io.IOUtils;

import com.google.common.base.Preconditions;
import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.Message;
import com.google.protobuf.TextFormat;

/**
 * A helper to load the protobuf message from a serialized text format.
 * Text file can be either on local filesystem or S3.
 *
 * @author Fei
 */
public class ProtoLoader {
  /**
   * Loads the protobuf message from a text file, containing the message in the serialized text format.
   * @param protoFile the file to load from.
   * @param builder an message builder to populate.
   * @param extensions an {@link ExtensionRegistry} in case a protobuf can contain protobuf extensions
   *    (https://developers.google.com/protocol-buffers/docs/proto#extensions).
   * @return The original message builder, filled with deserialized data from the file.
   * @throws {@link IOException} if there were failures.
   */
  public static <P extends Message.Builder> P load(String protoFile, P builder, ExtensionRegistry extensions)
      throws IOException {
    Preconditions.checkNotNull(protoFile);
    Preconditions.checkNotNull(builder);
    Preconditions.checkNotNull(extensions);
    BufferedReader in = null;
    try {
      in = new BufferedReader(new FileReader(protoFile));
      TextFormat.merge(in, extensions, builder);
    } finally {
      IOUtils.closeQuietly(in);
    }
    return builder;
  }

  /**
   * Loads the protobuf message from a text file, containing the message in the serialized text format.
   * This version assumes no extensions are available for the protobuf message.
   * @param protoFile the file to load from.
   * @param builder an message builder to populate.
   * @return The original message builder, filled with deserialized data from the file.
   * @throws {@link IOException} if there were failures.
   */
  public static <P extends Message.Builder> P load(String protoFile, P builder)
      throws IOException {
    return load(protoFile, builder, ExtensionRegistry.getEmptyRegistry());
  }
}
