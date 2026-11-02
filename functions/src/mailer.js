// Envío del código por correo con Gmail (contraseña de aplicación).
import nodemailer from 'nodemailer';
import { GMAIL_APP_PASSWORD, GMAIL_USER } from './config.js';

const TEXTS = {
  es: {
    subject: code => `Tu código de Traduya: ${code}`,
    body: (name, code) => `Hola ${name}:\n\nTu código para registrarte en Traduya es:\n\n    ${code}\n\n`
      + 'Vence en 10 minutos. Si no fuiste tú, ignora este correo.\n\nTraduya',
  },
  en: {
    subject: code => `Your Traduya code: ${code}`,
    body: (name, code) => `Hi ${name},\n\nYour code to sign up for Traduya is:\n\n    ${code}\n\n`
      + 'It expires in 10 minutes. If this wasn\'t you, ignore this email.\n\nTraduya',
  },
  zh: {
    subject: code => `你的 Traduya 验证码：${code}`,
    body: (name, code) => `${name}，你好：\n\n你注册 Traduya 的验证码是：\n\n    ${code}\n\n`
      + '验证码 10 分钟内有效。如果不是你本人操作，请忽略此邮件。\n\nTraduya',
  },
};

export async function sendCodeEmail({ to, name, code, lang }) {
  const text = TEXTS[lang] ?? TEXTS.es;
  const transport = nodemailer.createTransport({
    service: 'gmail',
    auth: { user: GMAIL_USER.value(), pass: GMAIL_APP_PASSWORD.value() },
  });
  await transport.sendMail({
    from: `Traduya <${GMAIL_USER.value()}>`,
    to,
    subject: text.subject(code),
    text: text.body(name, code),
  });
}
