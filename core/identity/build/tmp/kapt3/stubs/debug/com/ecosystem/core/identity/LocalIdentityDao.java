package com.ecosystem.core.identity;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000 \n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\u0003\bg\u0018\u00002\u00020\u0001J\u001a\u0010\u0002\u001a\u0004\u0018\u00010\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u0005H\u00a7@\u00a2\u0006\u0002\u0010\u0006J\u0016\u0010\u0007\u001a\u00020\b2\u0006\u0010\t\u001a\u00020\u0003H\u00a7@\u00a2\u0006\u0002\u0010\n\u00a8\u0006\u000b"}, d2 = {"Lcom/ecosystem/core/identity/LocalIdentityDao;", "", "getIdentity", "Lcom/ecosystem/core/identity/LocalIdentityEntity;", "alias", "", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "insertIdentity", "", "identity", "(Lcom/ecosystem/core/identity/LocalIdentityEntity;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "identity_debug"})
@androidx.room.Dao()
public abstract interface LocalIdentityDao {
    
    @androidx.room.Query(value = "SELECT * FROM local_identity WHERE idAlias = :alias LIMIT 1")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getIdentity(@org.jetbrains.annotations.NotNull()
    java.lang.String alias, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.ecosystem.core.identity.LocalIdentityEntity> $completion);
    
    @androidx.room.Insert(onConflict = 1)
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object insertIdentity(@org.jetbrains.annotations.NotNull()
    com.ecosystem.core.identity.LocalIdentityEntity identity, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 3, xi = 48)
    public static final class DefaultImpls {
    }
}