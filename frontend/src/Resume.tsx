import { useState } from "react";
import "./Resume.css";
import { backendUrl } from "./constants";

type ResumeResponse = {
  id: number;
  userId: string;
  fileName: string;
  contentType: string;
  sizeBytes: number;
  uploadedAt: string;
};

const Resume = () => {
  const [resume, setResume] = useState<File | null>(null);
  const [message, setMessage] = useState("");
  const [isUploading, setIsUploading] = useState(false);

  const validateAndSetResume = (selectedFile: File) => {
    const validExtensions = [".pdf", ".docx"];
    const fileName = selectedFile.name.toLowerCase();

    const isValidFile = validExtensions.some((extension) =>
      fileName.endsWith(extension)
    );

    if (!isValidFile) {
      setResume(null);
      setMessage("Please select a PDF or DOCX file.");
      return;
    }

    setResume(selectedFile);
    setMessage("Resume selected and ready to upload.");
  };

  const handleFileChange = (
    event: React.ChangeEvent<HTMLInputElement>
  ) => {
    const selectedFile = event.target.files?.[0];

    if (selectedFile) {
      validateAndSetResume(selectedFile);
    }
  };

  const handleDragOver = (
    event: React.DragEvent<HTMLDivElement>
  ) => {
    event.preventDefault();
  };

  const handleDrop = (
    event: React.DragEvent<HTMLDivElement>
  ) => {
    event.preventDefault();

    const droppedFile = event.dataTransfer.files?.[0];

    if (droppedFile) {
      validateAndSetResume(droppedFile);
    }
  };

  const handleUpload = async (
    event: React.FormEvent<HTMLFormElement>
  ) => {
    event.preventDefault();

    if (!resume) {
      setMessage("Please select a resume first.");
      return;
    }

    /*
      TEMPORARY:
      Replace this with the real logged-in userId
      once the authentication frontend is merged.
    */
    const userId = "TEMP_USER_ID";

    const formData = new FormData();
    formData.append("file", resume);

    try {
      setIsUploading(true);
      setMessage("Uploading resume...");

      const response = await fetch(
        `${backendUrl}/api/v1/resumes/users/${encodeURIComponent(userId)}`,
        {
          method: "POST",
          body: formData,
          credentials: "include",
        }
      );

      if (!response.ok) {
        const errorText = await response.text();
        setMessage(errorText || "Resume upload failed.");
        return;
      }

      const data: ResumeResponse = await response.json();

      setMessage(`Successfully uploaded ${data.fileName}.`);
    } catch (error) {
      console.error("Resume upload error:", error);
      setMessage("Could not connect to the backend.");
    } finally {
      setIsUploading(false);
    }
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
          <div
            className="resume-upload-box"
            onDragOver={handleDragOver}
            onDrop={handleDrop}
          >
            <label className="resume-upload-label">
              Drag and drop your resume here or select a file
            </label>

            <input
              type="file"
              accept=".pdf,.docx"
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
            disabled={isUploading}
          >
            {isUploading ? "Uploading..." : "Upload Resume"}
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