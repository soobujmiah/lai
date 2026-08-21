#include <android/log.h>
#include <dlfcn.h>
#include <jni.h>

#include <cstdint>
#include <cstring>
#include <string>
#include <vector>

namespace {
constexpr const char* kTag = "LAI-GPU-Probe";
using cl_int = int32_t; using cl_uint = uint32_t; using cl_platform_id = void*; using cl_device_id = void*;
using cl_context = void*; using cl_command_queue = void*; using cl_mem = void*; using cl_event = void*;
using cl_bitfield = uint64_t; using cl_device_type = cl_bitfield; using cl_mem_flags = cl_bitfield;
constexpr cl_int CL_SUCCESS = 0;
constexpr cl_device_type CL_DEVICE_TYPE_GPU = (1u << 2);
constexpr cl_uint CL_DEVICE_NAME = 0x102B, CL_DEVICE_VENDOR = 0x102C, CL_DEVICE_VERSION = 0x102F;
constexpr cl_uint CL_PLATFORM_NAME = 0x0902, CL_TRUE = 1;
constexpr cl_mem_flags CL_MEM_READ_WRITE = (1ull << 0);
constexpr cl_mem_flags CL_MEM_COPY_HOST_PTR = (1ull << 5);
using FnGetPlatformIDs = cl_int (*)(cl_uint, cl_platform_id*, cl_uint*);
using FnGetPlatformInfo = cl_int (*)(cl_platform_id, cl_uint, size_t, void*, size_t*);
using FnGetDeviceIDs = cl_int (*)(cl_platform_id, cl_device_type, cl_uint, cl_device_id*, cl_uint*);
using FnGetDeviceInfo = cl_int (*)(cl_device_id, cl_uint, size_t, void*, size_t*);
using FnCreateContext = cl_context (*)(const void*, cl_uint, const cl_device_id*, void*, void*, cl_int*);
using FnReleaseContext = cl_int (*)(cl_context);
using FnCreateCommandQueue = cl_command_queue (*)(cl_context, cl_device_id, cl_bitfield, cl_int*);
using FnReleaseCommandQueue = cl_int (*)(cl_command_queue);
using FnCreateBuffer = cl_mem (*)(cl_context, cl_mem_flags, size_t, void*, cl_int*);
using FnReleaseMemObject = cl_int (*)(cl_mem);
using FnEnqueueReadBuffer = cl_int (*)(cl_command_queue, cl_mem, cl_uint, size_t, size_t, void*, cl_uint, const cl_event*, cl_event*);
using FnFinish = cl_int (*)(cl_command_queue);
struct Api { void* handle=nullptr; FnGetPlatformIDs get_platform_ids=nullptr; FnGetPlatformInfo get_platform_info=nullptr; FnGetDeviceIDs get_device_ids=nullptr; FnGetDeviceInfo get_device_info=nullptr; FnCreateContext create_context=nullptr; FnReleaseContext release_context=nullptr; FnCreateCommandQueue create_command_queue=nullptr; FnReleaseCommandQueue release_command_queue=nullptr; FnCreateBuffer create_buffer=nullptr; FnReleaseMemObject release_mem_object=nullptr; FnEnqueueReadBuffer enqueue_read_buffer=nullptr; FnFinish finish=nullptr; };
template <typename T> bool load_symbol(void* h,const char* n,T& out){ out=reinterpret_cast<T>(dlsym(h,n)); if(!out) __android_log_print(ANDROID_LOG_ERROR,kTag,"missing OpenCL symbol: %s",n); return out!=nullptr; }
std::string device_string(FnGetDeviceInfo fn,cl_device_id d,cl_uint p){ size_t n=0; if(fn(d,p,0,nullptr,&n)!=CL_SUCCESS||!n)return{}; std::vector<char> v(n); return fn(d,p,n,v.data(),nullptr)==CL_SUCCESS?std::string(v.data()):std::string{}; }
std::string platform_string(FnGetPlatformInfo fn,cl_platform_id p,cl_uint param){ size_t n=0; if(fn(p,param,0,nullptr,&n)!=CL_SUCCESS||!n)return{}; std::vector<char> v(n); return fn(p,param,n,v.data(),nullptr)==CL_SUCCESS?std::string(v.data()):std::string{}; }
bool load_api(Api& a){ const char* candidates[]={"libOpenCL.so","/vendor/lib64/libOpenCL_adreno.so"}; for(const char* c:candidates){a.handle=dlopen(c,RTLD_NOW|RTLD_LOCAL); if(a.handle){__android_log_print(ANDROID_LOG_INFO,kTag,"OpenCL library loaded: %s",c);break;} __android_log_print(ANDROID_LOG_WARN,kTag,"dlopen('%s') failed: %s",c,dlerror());} if(!a.handle)return false;
#define LOAD(x) if(!load_symbol(a.handle,#x,a.x)) return false
LOAD(get_platform_ids); LOAD(get_platform_info); LOAD(get_device_ids); LOAD(get_device_info); LOAD(create_context); LOAD(release_context); LOAD(create_command_queue); LOAD(release_command_queue); LOAD(create_buffer); LOAD(release_mem_object); LOAD(enqueue_read_buffer); LOAD(finish);
#undef LOAD
return true; }
}

extern "C" JNIEXPORT jstring JNICALL Java_dev_lai_runtime_inference_NativeBindings_qualifyOpenCL(JNIEnv* env,jclass){
 Api api; if(!load_api(api)) return env->NewStringUTF("{\"status\":\"UNAVAILABLE\",\"reason\":\"dlopen failed\"}");
 cl_uint pc=0; cl_int rc=api.get_platform_ids(0,nullptr,&pc); if(rc!=CL_SUCCESS||pc==0){__android_log_print(ANDROID_LOG_WARN,kTag,"clGetPlatformIDs rc=%d count=%u",rc,pc);dlclose(api.handle);return env->NewStringUTF("{\"status\":\"NO_PLATFORM\"}");}
 std::vector<cl_platform_id> platforms(pc); if(api.get_platform_ids(pc,platforms.data(),nullptr)!=CL_SUCCESS){dlclose(api.handle);return env->NewStringUTF("{\"status\":\"PLATFORM_ENUM_FAILED\"}");}
 for(cl_platform_id platform:platforms){ const std::string pn=platform_string(api.get_platform_info,platform,CL_PLATFORM_NAME); __android_log_print(ANDROID_LOG_INFO,kTag,"OpenCL platform: %s",pn.c_str()); cl_uint dc=0; if(api.get_device_ids(platform,CL_DEVICE_TYPE_GPU,0,nullptr,&dc)!=CL_SUCCESS||dc==0)continue; std::vector<cl_device_id> devices(dc); if(api.get_device_ids(platform,CL_DEVICE_TYPE_GPU,dc,devices.data(),nullptr)!=CL_SUCCESS)continue;
  for(cl_device_id device:devices){ const std::string name=device_string(api.get_device_info,device,CL_DEVICE_NAME); const std::string vendor=device_string(api.get_device_info,device,CL_DEVICE_VENDOR); const std::string version=device_string(api.get_device_info,device,CL_DEVICE_VERSION); __android_log_print(ANDROID_LOG_INFO,kTag,"OpenCL GPU: name='%s' vendor='%s' version='%s'",name.c_str(),vendor.c_str(),version.c_str());
   cl_int err=CL_SUCCESS; cl_context ctx=api.create_context(nullptr,1,&device,nullptr,nullptr,&err); if(!ctx||err!=CL_SUCCESS)continue; cl_command_queue q=api.create_command_queue(ctx,device,0,&err); if(!q||err!=CL_SUCCESS){api.release_context(ctx);continue;}
   constexpr size_t bytes=4096; std::vector<uint8_t> in(bytes),out(bytes,0); for(size_t i=0;i<bytes;++i)in[i]=static_cast<uint8_t>(i*31u+7u); cl_mem b=api.create_buffer(ctx,CL_MEM_READ_WRITE|CL_MEM_COPY_HOST_PTR,bytes,in.data(),&err); if(!b||err!=CL_SUCCESS){api.release_command_queue(q);api.release_context(ctx);continue;}
   err=api.enqueue_read_buffer(q,b,CL_TRUE,0,bytes,out.data(),0,nullptr,nullptr); bool ok=(err==CL_SUCCESS&&std::memcmp(in.data(),out.data(),bytes)==0); err=api.finish(q); api.release_mem_object(b); api.release_command_queue(q); api.release_context(ctx); dlclose(api.handle);
   if(ok&&err==CL_SUCCESS){std::string json="{\"status\":\"COMPUTE_IO_VALIDATED\",\"platform\":\""+pn+"\",\"device\":\""+name+"\",\"vendor\":\""+vendor+"\",\"version\":\""+version+"\"}";return env->NewStringUTF(json.c_str());}
  }
 }
 return env->NewStringUTF("{\"status\":\"GPU_DEVICE_FOUND_BUT_IO_FAILED\"}");
}
