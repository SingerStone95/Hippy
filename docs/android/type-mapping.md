# ECMAScript 与 Java 类型映射

当通过 Bridge 进行调用时，Bridge 编解码器会自动将【源类型】转换成为【目标类型】供业务模块使用。

> 如 JS 调用 Java 时，会将传入的 ECMAScript 对象转换成为 Java 的对应类型，反之亦然。

Hippy 在 Java 中提供了两套类型系统：

* Recommennd Type System: 全新设计的类型系统，可以双向表达所有 [HTML structured clone algorithm](https://developer.mozilla.org/en-US/docs/Web/API/Web_Workers_API/Structured_clone_algorithm) 中支持的类型（推荐）。
* Compatible Type System: 旧类型系统，结构简单但无法完整（双向）表达类型，仅存量代码使用（不推荐）。

## 类型映射关系

| ECMAScript Type Category  | ECMAScript Type   | Recommennd(New) Type in Java                                         | Compatible(Old) Type in Java                                   |
|---------------------------|-------------------|----------------------------------------------------------------------|----------------------------------------------------------------|
| Primitives                | true              | true                                                                 | true                                                           |
|                           | false             | false                                                                | false                                                          |
|                           | null              | com.tencent.mtt.hippy.runtime.builtins.JSOddball#Null                | null                                                           |
|                           | undefined         | com.tencent.mtt.hippy.runtime.builtins.JSOddball#Undefined           | com.tencent.mtt.hippy.common.ConstantValue#Undefined           |
|                           | number            | int                                                                  | int                                                            |
|                           |                   | double                                                               | double                                                         |
|                           | bigint            | java.math.BigInteger                                                 | java.math.BigInteger                                           |
|                           | string            | java.lang.String                                                     | java.lang.String                                               |
| Primitive wrapper objects | Boolean           | com.tencent.mtt.hippy.runtime.builtins.objects.JSBooleanObject#True  | true                                                           |
|                           |                   | com.tencent.mtt.hippy.runtime.builtins.objects.JSBooleanObject#False | false                                                          |
|                           | Number            | com.tencent.mtt.hippy.runtime.builtins.objects.JSNumberObject        | double                                                         |
|                           | BigInt            | com.tencent.mtt.hippy.runtime.builtins.objects.JSBigintObject        | java.math.BigInteger                                           |
|                           | String            | com.tencent.mtt.hippy.runtime.builtins.objects.JSStringObject        | java.lang.String                                               |
| Fundamental objects       | Object            | com.tencent.mtt.hippy.runtime.builtins.JSObject                      | com.tencent.mtt.hippy.common.HippyMap                          |
| Indexed collections       | Array(dense)      | com.tencent.mtt.hippy.runtime.builtins.JSDenseArray                  | com.tencent.mtt.hippy.common.HippyArray  (Not fully supported) |
|                           | Array(sparse)     | com.tencent.mtt.hippy.runtime.builtins.JSSparseArray                 | N/A                                                            |
| Keyed collections         | Map               | com.tencent.mtt.hippy.runtime.builtins.JSMap                         | com.tencent.mtt.hippy.common.HippyMap (Not fully supported)    |
|                           | Set               | com.tencent.mtt.hippy.runtime.builtins.JSSet                         | com.tencent.mtt.hippy.common.HippyArray (Not fully supported)  |
| Structured data           | ArrayBuffer       | com.tencent.mtt.hippy.runtime.builtins.JSArrayBuffer                 | N/A                                                            |
|                           | SharedArrayBuffer | com.tencent.mtt.hippy.runtime.builtins.JSSharedArrayBuffer           | N/A                                                            |
|                           | ArrayBufferView   | com.tencent.mtt.hippy.runtime.builtins.JSDataView                    | N/A                                                            |
| Dates                     | Date              | java.util.Date                                                       | java.util.Date                                                 |
| Error objects             | Error             | com.tencent.mtt.hippy.runtime.builtins.JSError                       | N/A                                                            |
|                           | EvalError         |                                                                      |                                                                |
|                           | RangeError        |                                                                      |                                                                |
|                           | ReferenceError    |                                                                      |                                                                |
|                           | SyntaxError       |                                                                      |                                                                |
|                           | TypeError         |                                                                      |                                                                |
|                           | URIError          |                                                                      |                                                                |
| Text processing           | RegExp            | com.tencent.mtt.hippy.runtime.builtins.JSRegExp                      | N/A                                                            |
| Host Object               | \<any\>           | java.lang.Object                                                     | N/A                                                            |
| Array Holes               | undefined(hole)   | com.tencent.mtt.hippy.runtime.builtins.JSOddball#Hole                | com.tencent.mtt.hippy.common.ConstantValue#Hole                |


## 新旧类型互转

新旧类型系统间的类型互转可通过调用下述方法实现：

* Recommennd(New) ---> Compatible(Old): `com.tencent.mtt.hippy.runtime.utils.ValueConverter#toHippyValue`
* Compatible(Old) ---> Recommennd(New): `com.tencent.mtt.hippy.runtime.utils.ValueConverter#toJSValue`

> 由于新类型系统所能表达的 ECMAScript 类型更加精细
> __故从新类型系统（Recommennd）转换到旧类型系统（Compatible）时，可能会造成类型丢失__

一般情况下，用户无需进行新旧类型系统间的互转。模块在注册到 Bridge 时，可以指定所需使用的类型系统。

## Recommennd(New) Type in Java

所有新类型系统中的类型，均定义在 `com.tencent.mtt.hippy.runtime.builtins` 这个包中。

### JSValue

所有类型均派生于 `JSValue` 基类，主要提供了：

* 对象实例类型判定：可调用 `is##Name()` 方法来确认实际类型。
* 原始对象取值：可调用 `to##Name()` 方法得到 (wrapped)Primitives 的实际值。

### JSArray

所有类型数组（包括密集 `JSDenseArray` 与稀疏 `JSSparseArray` 数组）均派生于 `JSAbstractArray`。

根据 ECMAScript 规范的定义，`JSAbstractArray` 重写了由父类 `JSObject` 继承的方法（包含：`entries()`、`keys()`、`values()`）：

1. 会优先枚举数组内值。
2. 再枚举其属性值。

如您仅想遍历获取数组内的值，可调用 `items()` 方法。

### JSRegExp

由于 `java.util.regex.Pattern` 与 ECMAScript `RegExp` 对象的差异，故不支持全局（`g`）标志位与粘性匹配（`y`）标志位，而 Unicode 字符（`u`）标志位则始终启用。
