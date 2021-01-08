/*
 *
 * Tencent is pleased to support the open source community by making
 * Hippy available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.
 * All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

#include "core/napi/jsc/js_native_api_jsc.h"

#include <iostream>
#include <mutex>  // NOLINT(build/c++11)
#include <string>

#include "core/base/logging.h"
#include "core/napi/callback_info.h"
#include "core/napi/js_native_api.h"
#include "core/napi/jsc/js_native_jsc_helper.h"

namespace hippy {
namespace napi {

JSValueRef JsCallbackFunc(JSContextRef ctx,
                          JSObjectRef function,
                          JSObjectRef thisObject,
                          size_t argumentCount,
                          const JSValueRef arguments[],
                          JSValueRef* exception_ref) {
  void* data = JSObjectGetPrivate(function);
  if (!data) {
    return JSValueMakeUndefined(ctx);
  }
  FunctionData* fn_data = reinterpret_cast<FunctionData*>(data);
  std::shared_ptr<Scope> scope = fn_data->scope_.lock();
  if (!scope) {
    return JSValueMakeUndefined(ctx);
  }
  JsCallback cb = fn_data->callback_;
  std::shared_ptr<JSCCtx> context =
      std::static_pointer_cast<JSCCtx>(scope->GetContext());
  CallbackInfo info(scope);
  for (size_t i = 0; i < argumentCount; i++) {
    info.AddValue(
        std::make_shared<JSCCtxValue>(context->GetCtxRef(), arguments[i]));
  }
  cb(info);

  std::shared_ptr<JSCCtxValue> exception =
      std::static_pointer_cast<JSCCtxValue>(info.GetExceptionValue()->Get());
  if (exception) {
    *exception_ref = exception->value_;
    return JSValueMakeUndefined(ctx);
  }

  std::shared_ptr<JSCCtxValue> ret_value =
      std::static_pointer_cast<JSCCtxValue>(info.GetReturnValue()->Get());
  if (!ret_value) {
    return JSValueMakeUndefined(ctx);
  }

  JSValueRef valueRef = ret_value->value_;
  return valueRef;
}

JSObjectRef RegisterModule(std::shared_ptr<Scope> scope,
                           JSContextRef ctx,
                           std::string module_name,
                           ModuleClass module) {
  JSClassDefinition cls_def = kJSClassDefinitionEmpty;
  cls_def.className = module_name.c_str();
  JSClassRef cls_ref = JSClassCreate(&cls_def);
  JSObjectRef module_obj = JSObjectMake(ctx, cls_ref, nullptr);
  JSClassRelease(cls_ref);
  for (auto fn : module) {
    JSClassDefinition fn_def = kJSClassDefinitionEmpty;
    fn_def.className = fn.first.c_str();
    fn_def.callAsFunction = JsCallbackFunc;
    std::unique_ptr<FunctionData> fn_data =
        std::make_unique<FunctionData>(scope, fn.second);
    JSClassRef fn_ref = JSClassCreate(&fn_def);
    JSObjectRef fn_obj =
        JSObjectMake(ctx, fn_ref, reinterpret_cast<void*>(fn_data.get()));
    JSStringRef fn_str_ref = JSStringCreateWithUTF8CString(fn_def.className);
    JSObjectSetProperty(ctx, module_obj, fn_str_ref, fn_obj,
                        kJSPropertyAttributeReadOnly, nullptr);
    JSStringRelease(fn_str_ref);
    JSClassRelease(fn_ref);
    scope->SaveFunctionData(std::move(fn_data));
  }

  std::shared_ptr<JSCCtx> context =
      std::static_pointer_cast<JSCCtx>(scope->GetContext());
  std::shared_ptr<JSCCtxValue> module_value =
      std::make_shared<JSCCtxValue>(context->GetCtxRef(), module_obj);
  scope->AddModuleValue(module_name, module_value);
  return module_obj;
}

std::shared_ptr<VM> CreateVM() {
  return std::make_shared<JSCVM>();
}

void DetachThread() {}

void JSCVM::RegisterUncaughtExceptionCallback() {}

std::shared_ptr<Ctx> JSCVM::CreateContext() {
  return std::make_shared<JSCCtx>(vm_);
}

JSValueRef GetInternalBinding(JSContextRef ctx,
                              JSObjectRef function,
                              JSObjectRef thisObject,
                              size_t argc,
                              const JSValueRef argv[],
                              JSValueRef* exception) {
  if (argc <= 0) {
    return JSValueMakeNull(ctx);
  }

  JSValueRef name_ref = argv[0];
  if (!JSValueIsString(ctx, name_ref)) {
    return JSValueMakeNull(ctx);
  }

  BindingData* binding_data =
      reinterpret_cast<BindingData*>(JSObjectGetPrivate(function));
  std::shared_ptr<Scope> scope = binding_data->scope_.lock();
  if (!scope) {
    return JSValueMakeNull(ctx);
  }

  JSStringRef name_str_ref = JSValueToStringCopy(ctx, name_ref, nullptr);
  std::string module_name = JsStrToUTF8(name_str_ref);
  JSStringRelease(name_str_ref);

  std::shared_ptr<JSCCtxValue> module_value =
      std::static_pointer_cast<JSCCtxValue>(scope->GetModuleValue(module_name));
  if (module_value) {
    return module_value->value_;
  }

  ModuleClassMap module_class_map = binding_data->map_;
  auto it = module_class_map.find(module_name);
  if (it == module_class_map.end()) {
    return JSValueMakeNull(ctx);
  }

  return RegisterModule(scope, ctx, module_name, it->second);
}

std::shared_ptr<CtxValue> GetInternalBindingFn(std::shared_ptr<Scope> scope) {
  std::shared_ptr<JSCCtx> context =
      std::static_pointer_cast<JSCCtx>(scope->GetContext());
  JSClassDefinition cls_def = kJSClassDefinitionEmpty;
  cls_def.callAsFunction = GetInternalBinding;
  JSClassRef cls_ref = JSClassCreate(&cls_def);
  JSObjectRef functionObject = JSObjectMake(context->GetCtxRef(), cls_ref,
                                            scope->GetBindingData().get());
  JSClassRelease(cls_ref);
  std::shared_ptr<CtxValue> retValue =
      std::make_shared<JSCCtxValue>(context->GetCtxRef(), functionObject);
  return retValue;
}

bool JSCCtx::RegisterGlobalInJs() {
  JSStringRef global_ref = JSStringCreateWithUTF8CString("global");
  JSObjectSetProperty(context_, JSContextGetGlobalObject(context_), global_ref,
                      JSContextGetGlobalObject(context_),
                      kJSPropertyAttributeDontDelete, nullptr);
  JSStringRelease(global_ref);

  return true;
}

bool JSCCtx::SetGlobalJsonVar(const std::string& name,
                              const char* json,
                              std::string* exception) {
  JSObjectRef global_obj = JSContextGetGlobalObject(context_);
  JSStringRef name_ref = JSStringCreateWithUTF8CString(name.c_str());
  JSStringRef json_ref = JSStringCreateWithUTF8CString(json);
  JSValueRef value_ref = JSValueMakeFromJSONString(context_, json_ref);
  JSValueRef js_error = nullptr;
  JSObjectSetProperty(context_, global_obj, name_ref, value_ref,
                      kJSPropertyAttributeNone, &js_error);
  JSStringRelease(name_ref);
  JSStringRelease(json_ref);
  if (js_error && exception) {
    HandleJsException(js_error, *exception);
    return false;
  }
  return true;
}

bool JSCCtx::SetGlobalStrVar(const std::string& name,
                             const char* str,
                             std::string* exception) {
  JSObjectRef global_obj = JSContextGetGlobalObject(context_);
  JSStringRef name_ref = JSStringCreateWithUTF8CString(name.c_str());
  JSStringRef str_ref = JSStringCreateWithUTF8CString(str);
  JSValueRef value_ref = JSValueMakeString(context_, str_ref);
  JSValueRef js_error = nullptr;
  JSObjectSetProperty(context_, global_obj, name_ref, value_ref,
                      kJSPropertyAttributeNone, &js_error);
  JSStringRelease(name_ref);
  JSStringRelease(str_ref);
  if (js_error && exception) {
    HandleJsException(js_error, *exception);
    return false;
  }
  return true;
}

bool JSCCtx::SetGlobalObjVar(const std::string& name,
                             std::shared_ptr<CtxValue> obj,
                             std::string* exception) {
  JSObjectRef global_obj = JSContextGetGlobalObject(context_);
  JSStringRef name_ref = JSStringCreateWithUTF8CString(name.c_str());
  std::shared_ptr<JSCCtxValue> ctx_value =
      std::static_pointer_cast<JSCCtxValue>(obj);
  JSValueRef value_ref = ctx_value->value_;
  JSValueRef js_error = nullptr;
  JSObjectSetProperty(context_, global_obj, name_ref, value_ref,
                      kJSPropertyAttributeNone, &js_error);
  JSStringRelease(name_ref);
  if (js_error && exception) {
    HandleJsException(js_error, *exception);
    return false;
  }
  return true;
}

std::shared_ptr<CtxValue> JSCCtx::GetGlobalStrVar(const std::string& name,
                                                  std::string* exception) {
  JSObjectRef global_obj = JSContextGetGlobalObject(context_);
  JSStringRef name_ref = JSStringCreateWithUTF8CString(name.c_str());
  JSValueRef js_error = nullptr;
  JSValueRef value_ref =
      JSObjectGetProperty(context_, global_obj, name_ref, &js_error);
  bool is_str = JSValueIsString(context_, value_ref);
  JSStringRelease(name_ref);
  if (js_error && exception) {
    HandleJsException(js_error, *exception);
  }
  if (is_str) {
    return std::make_shared<JSCCtxValue>(context_, value_ref);
  }
  return nullptr;
}

std::shared_ptr<CtxValue> JSCCtx::GetGlobalObjVar(const std::string& name,
                                                  std::string* exception) {
  JSObjectRef global_obj = JSContextGetGlobalObject(context_);
  JSStringRef name_ref = JSStringCreateWithUTF8CString(name.c_str());
  JSValueRef js_error = nullptr;
  JSValueRef value_ref =
      JSObjectGetProperty(context_, global_obj, name_ref, &js_error);
  bool is_undefined = JSValueIsUndefined(context_, value_ref);
  JSStringRelease(name_ref);
  if (js_error && exception) {
    HandleJsException(js_error, *exception);
  }
  if (is_undefined) {
    return nullptr;
  } else {
    return std::make_shared<JSCCtxValue>(context_, value_ref);
  }
}

std::shared_ptr<CtxValue> JSCCtx::GetProperty(
    const std::shared_ptr<CtxValue> object,
    const std::string& name,
    std::string* exception) {
  CtxValue* value = object.get();
  JSCCtxValue* jsc_value = static_cast<JSCCtxValue*>(value);
  if (JSValueIsObject(context_, jsc_value->value_)) {
    JSObjectRef obj_ref = (JSObjectRef)jsc_value->value_;
    JSStringRef name_ref = JSStringCreateWithUTF8CString(name.c_str());
    JSValueRef js_error = nullptr;
    JSValueRef value_ref =
        JSObjectGetProperty(context_, obj_ref, name_ref, &js_error);
    bool is_undefined = JSValueIsUndefined(context_, value_ref);
    JSStringRelease(name_ref);
    if (js_error && exception) {
      HandleJsException(js_error, *exception);
    }
    if (is_undefined) {
      return nullptr;
    } else {
      return std::make_shared<JSCCtxValue>(context_, value_ref);
    }
  }
  return nullptr;
}

void JSCCtx::RegisterGlobalModule(std::shared_ptr<Scope> scope,
                                  const ModuleClassMap& module_cls_map) {
  std::shared_ptr<JSCCtx> ctx =
      std::static_pointer_cast<JSCCtx>(scope->GetContext());
  JSGlobalContextRef ctx_ref = ctx->GetCtxRef();
  for (const auto& module : module_cls_map) {
    RegisterModule(scope, ctx_ref, module.first, module.second);
  }
}

void JSCCtx::RegisterNativeBinding(const std::string& name,
                                   hippy::base::RegisterFunction fn,
                                   void* data) {
  return;
};

std::shared_ptr<CtxValue> JSCCtx::GetJsFn(const std::string& name,
                                          std::string* exception) {
  return GetGlobalObjVar(name, exception);
};

std::shared_ptr<CtxValue> JSCCtx::RunScript(const uint8_t* data,
                                            size_t len,
                                            const std::string& file_name,
                                            bool is_use_code_cache,
                                            std::string* cache,
                                            std::string* exception,
                                            Encoding encodeing) {
  if (!data || !len) {
    return nullptr;
  }

  JSStringRef js_string = JSStringCreateWithUTF8CString((const char*)data);
  JSValueRef js_error = nullptr;
  JSStringRef file_name_ref = nullptr;
  if (file_name.length() > 0) {
    file_name_ref = JSStringCreateWithUTF8CString(file_name.c_str());
  }
  JSValueRef value = JSEvaluateScript(context_, js_string, nullptr,
                                      file_name_ref, 1, &js_error);

  if (file_name_ref) {
    JSStringRelease(file_name_ref);
  }
  JSStringRelease(js_string);

  if (js_error) {
    HandleJsException(js_error, *exception);
  }

  if (!value) {
    return nullptr;
  }

  return std::make_shared<JSCCtxValue>(context_, value);
}

std::shared_ptr<CtxValue> JSCCtx::RunScript(const std::string&& script,
                                            const std::string& file_name,
                                            bool is_use_code_cache,
                                            std::string* cache,
                                            std::string* exception,
                                            Encoding encodeing) {
  return RunScript((const uint8_t*)script.c_str(), script.length(), file_name,
                   is_use_code_cache, cache, exception, encodeing);
}

}  // namespace napi
}  // namespace hippy
