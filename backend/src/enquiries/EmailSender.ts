export interface EmailAddress {
  name?: string;
  address: string;
}

export interface EmailMessage {
  to: string;
  from: string;
  replyTo?: EmailAddress;
  subject: string;
  text: string;
  html: string;
}

/** Boundary to the email provider (SMTP/Mailpit today; SES, SendGrid, Resend… later). Throws on failure. */
export interface EmailSender {
  send(message: EmailMessage): Promise<void>;
}
