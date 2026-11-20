import nodemailer, { type Transporter } from 'nodemailer';
import type { EmailMessage, EmailSender } from './EmailSender.js';

/** Sends via any SMTP server: Mailpit locally, AWS SES SMTP (or similar) in other environments. */
export class SmtpEmailSender implements EmailSender {
  private readonly transporter: Transporter;

  constructor(options: { host: string; port: number }) {
    this.transporter = nodemailer.createTransport({
      host: options.host,
      port: options.port,
      secure: false,
      connectionTimeout: 5000,
    });
  }

  async send(message: EmailMessage): Promise<void> {
    await this.transporter.sendMail(message);
  }
}
