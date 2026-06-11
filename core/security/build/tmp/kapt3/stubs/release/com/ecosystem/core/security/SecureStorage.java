package com.ecosystem.core.security;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0018\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0005\bf\u0018\u00002\u00020\u0001J\b\u0010\u0002\u001a\u00020\u0003H&J\u0012\u0010\u0004\u001a\u0004\u0018\u00010\u00052\u0006\u0010\u0006\u001a\u00020\u0005H&J\u0018\u0010\u0007\u001a\u00020\u00032\u0006\u0010\u0006\u001a\u00020\u00052\u0006\u0010\b\u001a\u00020\u0005H&J\u0010\u0010\t\u001a\u00020\u00032\u0006\u0010\u0006\u001a\u00020\u0005H&\u00a8\u0006\n"}, d2 = {"Lcom/ecosystem/core/security/SecureStorage;", "", "clear", "", "getString", "", "key", "putString", "value", "remove", "security_release"})
public abstract interface SecureStorage {
    
    public abstract void putString(@org.jetbrains.annotations.NotNull()
    java.lang.String key, @org.jetbrains.annotations.NotNull()
    java.lang.String value);
    
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.String getString(@org.jetbrains.annotations.NotNull()
    java.lang.String key);
    
    public abstract void remove(@org.jetbrains.annotations.NotNull()
    java.lang.String key);
    
    public abstract void clear();
}