INSERT INTO products (
    product_code,
    product_name,
    description,
    currency,
    min_age,
    allow_multiple_accounts,
    requires_manual_review,
    active
)
VALUES
(
    'CURRENT_ACCOUNT',
    'Current Account',
    'Tài khoản thanh toán dành cho khách hàng cá nhân',
    'VND',
    18,
    FALSE,
    FALSE,
    TRUE
),
(
    'SAVING_ACCOUNT',
    'Saving Account',
    'Tài khoản tiết kiệm thông thường',
    'VND',
    18,
    TRUE,
    TRUE,
    TRUE
),
(
    'DIGITAL_SAVING',
    'Digital Saving Account',
    'Tài khoản tiết kiệm mở và quản lý trực tuyến',
    'VND',
    18,
    TRUE,
    FALSE,
    TRUE
);