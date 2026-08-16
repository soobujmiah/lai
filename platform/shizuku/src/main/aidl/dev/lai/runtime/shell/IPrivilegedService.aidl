package dev.lai.runtime.shell;

interface IPrivilegedService {
    void destroy() = 16777114;
    String execute(in String[] argv, long timeoutMs, int outputLimit) = 1;
}
