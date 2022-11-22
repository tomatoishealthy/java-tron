package org.tron.trie;

import com.google.common.base.MoreObjects;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.Var;
import org.apache.tuweni.bytes.Bytes;
import org.immutables.value.Generated;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Immutable implementation of {@link InnerNodeDiscoveryManager.InnerNode}.
 * <p>
 * Use the builder to create immutable instances:
 * {@code ImmutableInnerNode.builder()}.
 */
@Generated(from = "InnerNodeDiscoveryManager.InnerNode", generator = "Immutables")
@SuppressWarnings({"all"})
@ParametersAreNonnullByDefault
@Immutable
@CheckReturnValue
public final class ImmutableInnerNode
    implements InnerNodeDiscoveryManager.InnerNode {
  private final Bytes location;
  private final Bytes path;

  private ImmutableInnerNode(Bytes location, Bytes path) {
    this.location = location;
    this.path = path;
  }

  /**
   * @return The value of the {@code location} attribute
   */
  @Override
  public Bytes location() {
    return location;
  }

  /**
   * @return The value of the {@code path} attribute
   */
  @Override
  public Bytes path() {
    return path;
  }

  /**
   * Copy the current immutable object by setting a value for the {@link InnerNodeDiscoveryManager.InnerNode#location() location} attribute.
   * A shallow reference equality check is used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for location
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableInnerNode withLocation(Bytes value) {
    if (this.location == value) return this;
    Bytes newValue = Objects.requireNonNull(value, "location");
    return new ImmutableInnerNode(newValue, this.path);
  }

  /**
   * Copy the current immutable object by setting a value for the {@link InnerNodeDiscoveryManager.InnerNode#path() path} attribute.
   * A shallow reference equality check is used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for path
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableInnerNode withPath(Bytes value) {
    if (this.path == value) return this;
    Bytes newValue = Objects.requireNonNull(value, "path");
    return new ImmutableInnerNode(this.location, newValue);
  }

  /**
   * This instance is equal to all instances of {@code ImmutableInnerNode} that have equal attribute values.
   * @return {@code true} if {@code this} is equal to {@code another} instance
   */
  @Override
  public boolean equals(@Nullable Object another) {
    if (this == another) return true;
    return another instanceof ImmutableInnerNode
        && equalTo(0, (ImmutableInnerNode) another);
  }

  private boolean equalTo(int synthetic, ImmutableInnerNode another) {
    return location.equals(another.location)
        && path.equals(another.path);
  }

  /**
   * Computes a hash code from attributes: {@code location}, {@code path}.
   * @return hashCode value
   */
  @Override
  public int hashCode() {
    @Var int h = 5381;
    h += (h << 5) + location.hashCode();
    h += (h << 5) + path.hashCode();
    return h;
  }

  /**
   * Prints the immutable value {@code InnerNode} with attribute values.
   * @return A string representation of the value
   */
  @Override
  public String toString() {
    return MoreObjects.toStringHelper("InnerNode")
        .omitNullValues()
        .add("location", location)
        .add("path", path)
        .toString();
  }

  /**
   * Creates an immutable copy of a {@link InnerNodeDiscoveryManager.InnerNode} value.
   * Uses accessors to get values to initialize the new immutable instance.
   * If an instance is already immutable, it is returned as is.
   * @param instance The instance to copy
   * @return A copied immutable InnerNode instance
   */
  public static ImmutableInnerNode copyOf(InnerNodeDiscoveryManager.InnerNode instance) {
    if (instance instanceof ImmutableInnerNode) {
      return (ImmutableInnerNode) instance;
    }
    return ImmutableInnerNode.builder()
        .from(instance)
        .build();
  }

  /**
   * Creates a builder for {@link ImmutableInnerNode ImmutableInnerNode}.
   * <pre>
   * ImmutableInnerNode.builder()
   *    .location(org.apache.tuweni.bytes.Bytes) // required {@link InnerNodeDiscoveryManager.InnerNode#location() location}
   *    .path(org.apache.tuweni.bytes.Bytes) // required {@link InnerNodeDiscoveryManager.InnerNode#path() path}
   *    .build();
   * </pre>
   * @return A new ImmutableInnerNode builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builds instances of type {@link ImmutableInnerNode ImmutableInnerNode}.
   * Initialize attributes and then invoke the {@link #build()} method to create an
   * immutable instance.
   * <p><em>{@code Builder} is not thread-safe and generally should not be stored in a field or collection,
   * but instead used immediately to create instances.</em>
   */
  @Generated(from = "InnerNodeDiscoveryManager.InnerNode", generator = "Immutables")
  @NotThreadSafe
  public static final class Builder {
    private static final long INIT_BIT_LOCATION = 0x1L;
    private static final long INIT_BIT_PATH = 0x2L;
    private long initBits = 0x3L;

    private @Nullable Bytes location;
    private @Nullable Bytes path;

    private Builder() {
    }

    /**
     * Fill a builder with attribute values from the provided {@code InnerNode} instance.
     * Regular attribute values will be replaced with those from the given instance.
     * Absent optional values will not replace present values.
     * @param instance The instance from which to copy values
     * @return {@code this} builder for use in a chained invocation
     */
    @CanIgnoreReturnValue 
    public final Builder from(InnerNodeDiscoveryManager.InnerNode instance) {
      Objects.requireNonNull(instance, "instance");
      location(instance.location());
      path(instance.path());
      return this;
    }

    /**
     * Initializes the value for the {@link InnerNodeDiscoveryManager.InnerNode#location() location} attribute.
     * @param location The value for location 
     * @return {@code this} builder for use in a chained invocation
     */
    @CanIgnoreReturnValue 
    public final Builder location(Bytes location) {
      this.location = Objects.requireNonNull(location, "location");
      initBits &= ~INIT_BIT_LOCATION;
      return this;
    }

    /**
     * Initializes the value for the {@link InnerNodeDiscoveryManager.InnerNode#path() path} attribute.
     * @param path The value for path 
     * @return {@code this} builder for use in a chained invocation
     */
    @CanIgnoreReturnValue 
    public final Builder path(Bytes path) {
      this.path = Objects.requireNonNull(path, "path");
      initBits &= ~INIT_BIT_PATH;
      return this;
    }

    /**
     * Builds a new {@link ImmutableInnerNode ImmutableInnerNode}.
     * @return An immutable instance of InnerNode
     * @throws IllegalStateException if any required attributes are missing
     */
    public ImmutableInnerNode build() {
      if (initBits != 0) {
        throw new IllegalStateException(formatRequiredAttributesMessage());
      }
      return new ImmutableInnerNode(location, path);
    }

    private String formatRequiredAttributesMessage() {
      List<String> attributes = new ArrayList<>();
      if ((initBits & INIT_BIT_LOCATION) != 0) attributes.add("location");
      if ((initBits & INIT_BIT_PATH) != 0) attributes.add("path");
      return "Cannot build InnerNode, some of required attributes are not set " + attributes;
    }
  }
}
