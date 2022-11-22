/*
 * Copyright contributors to Hyperledger Besu
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.tron.trie;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.bytes.MutableBytes;
import org.apache.tuweni.bytes.MutableBytes32;
import org.tron.rlp.RLPInput;
import org.immutables.value.Value;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

public class InnerNodeDiscoveryManager<V> extends StoredNodeFactory<V> {

  private final List<InnerNode> innerNodes = new ArrayList<>();

  private final Bytes startKeyHash, endKeyHash;

  private final boolean allowMissingElementInRange;

  public InnerNodeDiscoveryManager(
      final NodeLoader nodeLoader,
      final Function<V, Bytes> valueSerializer,
      final Function<Bytes, V> valueDeserializer,
      final Bytes32 startKeyHash,
      final Bytes32 endKeyHash,
      final boolean allowMissingElementInRange) {
    super(nodeLoader, valueSerializer, valueDeserializer);
    this.startKeyHash = createPath(startKeyHash);
    this.endKeyHash = createPath(endKeyHash);
    this.allowMissingElementInRange = allowMissingElementInRange;
  }

  @Override
  protected Node<V> decodeExtension(
      final Bytes location,
      final Bytes path,
      final RLPInput valueRlp,
      final Supplier<String> errMessage) {
    final ExtensionNode<V> vNode =
        (ExtensionNode<V>) super.decodeExtension(location, path, valueRlp, errMessage);
    if (isInRange(Bytes.concatenate(location, Bytes.of(0)))) {
      innerNodes.add(
          ImmutableInnerNode.builder()
              .location(location)
              .path(Bytes.of(0, CompactEncoding.LEAF_TERMINATOR))
              .build());
    }
    return vNode;
  }

  @Override
  protected BranchNode<V> decodeBranch(
      final Bytes location, final RLPInput nodeRLPs, final Supplier<String> errMessage) {
    final BranchNode<V> vBranchNode = super.decodeBranch(location, nodeRLPs, errMessage);
    final List<Node<V>> children = vBranchNode.getChildren();
    for (int i = 0; i < children.size(); i++) {
      if (isInRange(Bytes.concatenate(location, Bytes.of(i)))) {
        innerNodes.add(
            ImmutableInnerNode.builder()
                .location(location)
                .path(Bytes.of(i, CompactEncoding.LEAF_TERMINATOR))
                .build());
      }
    }
    return vBranchNode;
  }

  @Override
  protected LeafNode<V> decodeLeaf(
      final Bytes location,
      final Bytes path,
      final RLPInput valueRlp,
      final Supplier<String> errMessage) {
    final LeafNode<V> vLeafNode = super.decodeLeaf(location, path, valueRlp, errMessage);
    final Bytes concatenatePath = Bytes.concatenate(location, path);
    if (isInRange(concatenatePath.slice(0, concatenatePath.size() - 1))) {
      innerNodes.add(ImmutableInnerNode.builder().location(location).path(path).build());
    }
    return vLeafNode;
  }

  @Override
  public Optional<Node<V>> retrieve(final Bytes location, final Bytes32 hash)
      throws MerkleTrieException {
//    return super.retrieve(location, hash)   // todo to be delete
//        .or(
//            () -> {
//              if (!allowMissingElementInRange && isInRange(location)) {
//                return Optional.empty();
//              }
//              return Optional.of(new MissingNode<>(hash, location));
//            });
    Optional<Node<V>> result = super.retrieve(location, hash);
    if (result.isPresent()) {
      return result;
    } else {
      if (!allowMissingElementInRange && isInRange(location)) {
        return Optional.empty();
      }
      return Optional.of(new MissingNode<>(hash, location));
    }
  }

  public List<InnerNode> getInnerNodes() {
//    return List.copyOf(innerNodes);  // todo to be delete
    return Collections.unmodifiableList(innerNodes);
  }

  private boolean isInRange(final Bytes location) {
    return !location.isEmpty()
//        && Arrays.compare(location.toArrayUnsafe(), startKeyHash.toArrayUnsafe()) >= 0  // todo to be delete
//        && Arrays.compare(location.toArrayUnsafe(), endKeyHash.toArrayUnsafe()) <= 0;
        && compare(location.toArrayUnsafe(), startKeyHash.toArrayUnsafe()) >= 0
        && compare(location.toArrayUnsafe(), endKeyHash.toArrayUnsafe()) <= 0;
  }

  public static int compare(byte[] a, byte[] b) {
    if (a == b)
      return 0;
    if (a == null || b == null)
      return a == null ? -1 : 1;

    int endIndex = Math.min(a.length, b.length);
    for (int i = 0; i < endIndex; i++) {
      int result = Byte.compare(a[i], b[i]);
      if (result != 0) {
        return result;
      }
    }
    return a.length - b.length;
  }

  private Bytes createPath(final Bytes bytes) {
    final MutableBytes path = MutableBytes.create(bytes.size() * 2);
    int j = 0;
    for (int i = 0; i < bytes.size(); i += 1, j += 2) {
      final byte b = bytes.get(i);
      path.set(j, (byte) ((b >>> 4) & 0x0f));
      path.set(j + 1, (byte) (b & 0x0f));
    }
    return path;
  }

//  public static Bytes32 decodePath(final Bytes bytes) {
//    final MutableBytes32 decoded = MutableBytes32.create();
//    final MutableBytes path = MutableBytes.create(Bytes32.SIZE * 2);
//    path.set(0, bytes);
//    int decodedPos = 0;
//    for (int pathPos = 0; pathPos < path.size() - 1; pathPos += 2, decodedPos += 1) {
//      final byte high = path.get(pathPos);
//      final byte low = path.get(pathPos + 1);
//      if ((high & 0xf0) != 0 || (low & 0xf0) != 0) {
//        throw new IllegalArgumentException("Invalid path: contains elements larger than a nibble");
//      }
//      decoded.set(decodedPos, (byte) (high << 4 | (low & 0xff)));
//    }
//    return decoded;
//  }

  @Value.Immutable
  public interface InnerNode {
    Bytes location();

    Bytes path();
  }
}
