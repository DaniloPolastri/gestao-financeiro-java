package com.findash.shared.security;

import java.util.UUID;

public final class CompanyContextHolder {

    private static final ThreadLocal<UUID> COMPANY_ID = new ThreadLocal<>();

    private CompanyContextHolder() {}

    public static void set(UUID companyId) { COMPANY_ID.set(companyId); }
    public static UUID get() { return COMPANY_ID.get(); }
    public static void clear() { COMPANY_ID.remove(); }
}
