# Release Notes

## 0.0.3

* Un-deprecated `ChannelFactory` and changed purpose of the class
* Refactored `ZmqTemplate` to use `ChannelFactory`
* Renamed `Channels` to `ChannelProxy` and made public
* Made `ZmqHandlerInvoker` an `internal` class
* Improved method signature matching in `ZmqHandlerInvoker`

## 0.0.2

* Added `headers` parameter to `MessageConverter.toMessage()`
* Added `SimpleMessageConverter` class supporting `String` and `ByteArray`
* Removed `DefaultMessageConverter` in favor of `SimpleMessageConverter`
* Added `ZmqTemplate` class with basic `send()` operations
* Deprecated `ChannelFactory` which is superceded by `ZmqTemplate`
* `Channel` now implements `Closeable`

## 0.0.1

* Initial release