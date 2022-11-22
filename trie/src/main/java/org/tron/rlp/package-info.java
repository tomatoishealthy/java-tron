/* SPDX-License-Identifier: Apache-2.0 */
/**
 * Recursive Length Prefix (RLP) encoding and decoding.
 *
 * <p>This package provides encoding and decoding of data with the RLP encoding scheme. Encoding is
 * done through writing data to a {@link RLPOutput} (for instance
 * {@link BytesValueRLPOutput}, which then exposes the encoded
 * output as a {@link org.apache.tuweni.bytes.Bytes} through {@link
 * BytesValueRLPOutput#encoded()}). Decoding is done by wrapping
 * encoded data in a {@link RLPInput} (using, for instance, {@link
 * RLP#input}) and reading from it.
 */
package org.tron.rlp;
