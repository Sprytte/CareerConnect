import { useState } from "react";
import "./Resume.css";

const Resume = () => {
  const [resume, setResume] = useState<File | null>(null);
  const [message, setMessage] = useState("");

  const handleFileChange = (
    event: React.ChangeEvent<HTMLInputElement>
  ) => {
    const selectedFile = event.target.files?.[0];

    if (selectedFile) {
      setResume(selectedFile);
      setMessage("Resume selected and ready to upload.");
    }
  };

  const handleUpload = (
    event: React.FormEvent<HTMLFormElement>
  ) => {
    event.preventDefault();

    if (!resume) {
      setMessage("Please select a resume first.");
      return;
    }

    // Backend API upload will be added here later.
  };

  return (
    <section className="resume">
      <div className="resume-container">
        <h2 className="resume-title">Resume Management</h2>

        <p className="resume-subtitle">
          Upload your resume so you can use it when applying for jobs through
          CareerConnect.
        </p>

        <form onSubmit={handleUpload}>
          <div className="resume-upload-box">
            <label className="resume-upload-label">
              Upload your resume
            </label>

            <input
              type="file"
              accept=".pdf"
              onChange={handleFileChange}
            />
          </div>

          {resume && (
            <div className="resume-selected-file">
              Selected file: <strong>{resume.name}</strong>
            </div>
          )}

          <button
            className="resume-upload-button"
            type="submit"
          >
            Upload Resume
          </button>

          {message && (
            <p className="resume-message">{message}</p>
          )}
        </form>
      </div>
    </section>
  );
};

export default Resume;