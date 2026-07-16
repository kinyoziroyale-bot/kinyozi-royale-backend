package com.kinyozi.royale.security;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.UUID;

public class CurrentUser {
  public static String username(){
    var a = SecurityContextHolder.getContext().getAuthentication();
    return a == null ? null : (String) a.getPrincipal();
  }
  public static UUID tenantId(){
    var a = SecurityContextHolder.getContext().getAuthentication();
    if (a == null || a.getDetails() == null) throw new RuntimeException("No tenant in context");
    return UUID.fromString(a.getDetails().toString());
  }
}
